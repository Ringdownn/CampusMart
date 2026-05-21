package searcher

import (
	"campusmart/search-engine/internal/searcher/arrays"
	"campusmart/search-engine/internal/searcher/model"
	"campusmart/search-engine/internal/searcher/pagination"
	"campusmart/search-engine/internal/searcher/sorts"
	"campusmart/search-engine/internal/searcher/storage"
	"campusmart/search-engine/internal/searcher/utils"
	"campusmart/search-engine/internal/searcher/words"
	"errors"
	"fmt"
	"log"
	"os"
	"runtime"
	"sync"
	"time"
)

type Engine struct {
	IndexPath string
	Option    *Option

	invertedIndexStorages []*storage.LevelDBStorage
	positiveIndexStorages []*storage.LevelDBStorage
	docStorages           []*storage.LevelDBStorage

	sync.Mutex
	sync.WaitGroup
	addDocumentWorkerChan []chan *model.IndexDoc //添加索引的通道
	IsDebug               bool
	Tokenizer             *words.Tokenizer //分词器
	DatabaseName          string           //数据库名

	Shard     int   //分片数
	Timeout   int64 //超时时间
	BufferNum int   //分片缓冲数

	documentCount int64
}

type Option struct {
	InvertedIndexName string //倒排索引路径
	PositiveIndexName string //正排索引路径
	DocIndexName      string //文档路径
}

//初始化引擎

// InitOption 初始化默认配置
func (e *Engine) InitOption(option *Option) {
	if option == nil {
		option = e.GetOptions()
	}
	if e.Shard <= 0 {
		e.Shard = 10
	}
	if e.BufferNum <= 0 {
		e.BufferNum = 1000
	}

	//初始化引擎
	e.Init()
}

func (e *Engine) Init() {
	//加等待
	e.Add(1)
	defer e.Done()

	if e.Option == nil {
		e.Option = e.GetOptions()
	}
	if e.Timeout == 0 {
		e.Timeout = 30
	}

	e.documentCount = -1
	log.Print("chain num:", e.BufferNum*e.Shard)

	e.addDocumentWorkerChan = make([]chan *model.IndexDoc, e.Shard)

	for shard := 0; shard < e.Shard; shard++ {
		//初始化chan
		worker := make(chan *model.IndexDoc, e.BufferNum)
		e.addDocumentWorkerChan[shard] = worker

		go e.DocumentWorkerExec(worker)

		//初始化文档储存
		s, err := storage.NewLevelDBStorage(e.getFilePath(fmt.Sprintf("%s_%d", e.Option.DocIndexName, shard)), e.Timeout)
		if err != nil {
			panic(err)
		}
		e.docStorages = append(e.docStorages, s)

		//初始化key储存
		ks, kerr := storage.NewLevelDBStorage(e.getFilePath(fmt.Sprintf("%s_%d", e.Option.InvertedIndexName, shard)), e.Timeout)
		if kerr != nil {
			panic(kerr)
		}
		e.invertedIndexStorages = append(e.invertedIndexStorages, ks)

		//id和key映射
		iks, ikerr := storage.NewLevelDBStorage(e.getFilePath(fmt.Sprintf("%s_%d", e.Option.PositiveIndexName, shard)), e.Timeout)
		if ikerr != nil {
			panic(ikerr)
		}
		e.positiveIndexStorages = append(e.positiveIndexStorages, iks)
	}
	go e.automaticGC()
}

// 每十秒进行一次手动垃圾回收
func (e *Engine) automaticGC() {
	ticker := time.NewTicker(time.Second * time.Duration(10))
	for {
		<-ticker.C
		runtime.GC()
	}
}

func (e *Engine) GetOptions() *Option {
	return &Option{
		InvertedIndexName: "inverted_index",
		PositiveIndexName: "positive_index",
		DocIndexName:      "docs",
	}
}

func (e *Engine) getFilePath(filename string) string {
	return e.IndexPath + string(os.PathSeparator) + filename
}

//计算分片逻辑

// GetShard 计算分片
func (e *Engine) GetShard(id uint32) int {
	return int(id % uint32(e.Shard))
}

// GetShardByWord 通过string计算分片
func (e *Engine) GetShardByWord(word string) int {
	return int(utils.StringToInt(word) % uint32(e.Shard))
}

//文件处理流程

// DocumentWorkerExec 添加文档队列
func (e *Engine) DocumentWorkerExec(worker chan *model.IndexDoc) {
	for {
		doc := <-worker
		e.AddDocument(doc)
	}
}

func (e *Engine) IndexDocument(document *model.IndexDoc) error {
	e.documentCount++
	e.addDocumentWorkerChan[e.GetShard(document.Id)] <- document
	return nil
}

// AddDocument 分词索引
func (e *Engine) AddDocument(index *model.IndexDoc) {
	e.Wait()
	text := index.Text

	splitWords := e.Tokenizer.Cut(text)

	//如果有图片打标后到标签，将标签加入分词结果
	if len(index.Tags) > 0 {
		splitWords = append(splitWords, index.Tags...)
	}

	id := index.Id
	//检测是否需要更新倒排索引，id不存在/索引不存在
	inserts, needUpdateInverted := e.optimizeIndex(id, splitWords)
	if needUpdateInverted {
		for _, word := range inserts {
			e.addInvertedIndex(word, id)
		}
	}

	e.addPositiveIndex(index, splitWords)
}

// 添加正排索引
func (e *Engine) addPositiveIndex(index *model.IndexDoc, keys []string) {
	e.Lock()
	defer e.Unlock()

	key := utils.Uint32ToByte(index.Id) //Id的字节表示，方便存储
	shard := e.GetShard(index.Id)

	//获取储存对象
	docStorage := e.docStorages[shard]
	positiveIndexStorage := e.positiveIndexStorages[shard]

	//创建文档
	doc := &model.StorageIndexDoc{
		IndexDoc: index, //	原始文档
		Keys:     keys,  //关键词列表
	}

	//存储Id（key）和文档的映射
	docStorage.Set(key, utils.Encoder(doc))
	//存储Id（key）和关键词的映射
	positiveIndexStorage.Set(key, utils.Encoder(keys))

}

// 添加倒排索引
func (e *Engine) addInvertedIndex(word string, id uint32) {
	e.Lock()
	defer e.Unlock()

	shard := e.GetShardByWord(word)

	//获取存储对象
	s := e.invertedIndexStorages[shard]

	//word作为key
	key := []byte(word)

	//如果存在，取出文档列表并放在ids中
	buf, find := s.Get(key)
	ids := make([]uint32, 0)
	if find {
		utils.Decoder(buf, &ids)
	}

	//若文档列表中不存在，添加到列表中
	if !arrays.ArrayUint32Exists(ids, id) {
		ids = append(ids, id)
	}

	s.Set(key, utils.Encoder(ids))
}

// 检测是否需要更新, 并移除删去的词
func (e *Engine) optimizeIndex(id uint32, newWords []string) ([]string, bool) {
	e.Lock()
	defer e.Unlock()

	//计算差值
	removes, inserts, changed := e.getDifference(id, newWords)
	//println(removes, inserts, changed)
	if changed {
		//println(removes)
		if removes != nil && len(removes) > 0 {
			for _, word := range removes {
				//println(word)
				e.removeIdInWordIndex(id, word)
			}
		}
	}
	return inserts, changed
}

func (e *Engine) removeIdInWordIndex(id uint32, word string) {
	shard := e.GetShardByWord(word)
	s := e.invertedIndexStorages[shard]

	key := []byte(word)
	buf, find := s.Get(key)
	ids := make([]uint32, 0)
	if find {
		utils.Decoder(buf, &ids)

		//移除对应的词
		index := arrays.Find(ids, id)
		if index != -1 {
			ids = utils.DeleteArray(ids, index)
			if len(ids) == 0 {
				err := s.Delete(key)
				if err != nil {
					panic(err)
				}
			} else {
				s.Set(key, utils.Encoder(ids))
			}
		}
	}
}

// 计算差值
// 返回的第一个数组为需要删除的词，第二个为需要添加的词，第三个为是否改变
func (e *Engine) getDifference(id uint32, newWords []string) ([]string, []string, bool) {
	shard := e.GetShard(id)
	s := e.positiveIndexStorages[shard]
	key := utils.Uint32ToByte(id)
	buf, found := s.Get(key)
	if found {
		oldWords := make([]string, 0)
		utils.Decoder(buf, &oldWords)

		//计算需要移除的
		remove := make([]string, 0)
		for _, word := range oldWords {
			if !arrays.ArrayStringExists(newWords, word) {
				remove = append(remove, word)
			}
		}

		//计算需要新增的
		inserts := make([]string, 0)
		for _, word := range newWords {
			if !arrays.ArrayStringExists(oldWords, word) {
				inserts = append(inserts, word)
			}
		}

		if len(inserts) != 0 || len(remove) != 0 {
			//存在变化
			return remove, inserts, true
		}
		return remove, inserts, false
	}
	//id不存在，相当于新增
	return nil, newWords, true
}

// MultiSearch 多线程搜索
func (e *Engine) MultiSearch(request *model.SearchRequest) (*model.SearchResult, error) {
	//等待引擎初始化完成
	e.Wait()

	//分词
	words := e.Tokenizer.CutWithTranslate(request.Query)

	//并行查询到排索引
	fastSort := &sorts.FastSort{
		IsDebug: e.IsDebug,
		Order:   request.Order,
	}

	_time := utils.ExecTime(func() {
		base := len(words)
		wg := &sync.WaitGroup{}
		wg.Add(base)

		for _, word := range words {
			go e.processKeySearch(word, fastSort, wg)
		}
		wg.Wait()
	})

	//处理分页
	request = request.GetAndSetDefault()

	//计算交集得分和去重
	fastSort.Process()

	wordMap := make(map[string]bool)
	for _, word := range words {
		wordMap[word] = true
	}

	var result = &model.SearchResult{
		Total: fastSort.Count(),
		Page:  request.Page,
		Limit: request.Limit,
		Words: words,
	}

	//处理数据
	t, err := utils.ExecTimeWithError(func() error {
		pager := new(pagination.Pagination)
		pager.Init(request.Limit, fastSort.Count())
		//设置总页数
		result.PageCount = pager.PageCount

		//读取单页的id
		if pager.PageCount != 0 {
			//TODO自定义分数表达式
			//TODO分数表达式不为空，获取所有数据
			//TODO根据自定义分数表达式计算排名

			start, end := pager.GetPage(request.Page)

			var resultItems = make([]model.SliceItem, 0)
			fastSort.GetAll(&resultItems, start, end)

			count := len(resultItems)

			result.Documents = make([]model.ResponseDoc, count)
			wg := &sync.WaitGroup{}
			wg.Add(count)
			for index, item := range resultItems {
				go e.getDocument(item, &result.Documents[index], wg)
			}
			wg.Wait()
		}
		//无数据
		return nil
	})
	if err != nil {
		return nil, err
	}
	result.Time = _time + t

	return result, nil
}

// 通过Id获取文档并换填到响应
func (e *Engine) getDocument(item model.SliceItem, doc *model.ResponseDoc, wg *sync.WaitGroup) {
	buf := e.GetDocById(item.Id)
	defer wg.Done()
	doc.Score = item.Score

	if buf != nil {
		//gob解析
		storageDoc := new(model.StorageIndexDoc)
		utils.Decoder(buf, &storageDoc)
		//取出文档
		doc.Document = storageDoc.Document
		doc.Keys = storageDoc.Keys
		doc.Text = storageDoc.Text
		doc.Id = item.Id
		// TODO关键词高亮
	}
}

func (e *Engine) GetDocById(id uint32) []byte {
	shard := e.GetShard(id)
	s := e.docStorages[shard]
	key := utils.Uint32ToByte(id)

	buf, find := s.Get(key)
	if find {
		return buf
	}
	return nil
}

// 通过倒排索引+词搜索文档，并加到fastSort的缓冲区
func (e *Engine) processKeySearch(word string, fastSort *sorts.FastSort, wg *sync.WaitGroup) {
	defer wg.Done()

	shard := e.GetShardByWord(word)
	s := e.invertedIndexStorages[shard]
	key := []byte(word)

	buf, find := s.Get(key)
	if find {
		ids := make([]uint32, 0)
		utils.Decoder(buf, &ids)
		fastSort.Add(&ids)
	}

}

func (e *Engine) GetIndexCount() int64 {
	var size int64
	for i := 0; i < e.Shard; i++ {
		size += e.invertedIndexStorages[i].GetCount()
	}
	return size
}

func (e *Engine) GetDocumentCount() int64 {
	if e.documentCount == -1 {
		var size int64
		//多线程加速
		wg := &sync.WaitGroup{}
		wg.Add(e.Shard)
		for i := 0; i < e.Shard; i++ {
			go func(i int) {
				size += e.docStorages[i].GetCount()
				wg.Done()
			}(i)
		}
		wg.Wait()
		e.documentCount = size
	}
	return e.documentCount
}

// 根据Id移除索引remove
func (e *Engine) RemoveIndex(id uint32) error {
	e.Lock()
	defer e.Unlock()

	shard := e.GetShard(id)
	key := utils.Uint32ToByte(id)

	//通过正排索引拿到keys
	ps := e.positiveIndexStorages[shard]
	keysBuf, find := ps.Get(key)
	if !find {
		return errors.New(fmt.Sprintf("index not found: %d", id))
	}

	keys := make([]string, 0)
	utils.Decoder(keysBuf, &keys)

	//删除倒排索引中的文件ID
	for _, word := range keys {
		e.removeIdInWordIndex(id, word)
	}

	//删除正排索引
	err := ps.Delete(key)
	if err != nil {
		return err
	}

	//删除文档
	err = e.docStorages[shard].Delete(key)
	if err != nil {
		return err
	}
	//减少文档数量
	e.documentCount--
	return nil
}

// Close 关闭引擎
func (e *Engine) Close() {
	e.Lock()
	defer e.Unlock()

	for i := 0; i < e.Shard; i++ {
		_ = e.docStorages[i].Close()
		_ = e.invertedIndexStorages[i].Close()
		_ = e.positiveIndexStorages[i].Close()
	}
}

// Drop 删除文件
func (e *Engine) Drop() error {
	e.Lock()
	defer e.Unlock()

	if err := os.RemoveAll(e.IndexPath); err != nil {
		return err
	}

	e.docStorages = make([]*storage.LevelDBStorage, 0)
	e.invertedIndexStorages = make([]*storage.LevelDBStorage, 0)
	e.positiveIndexStorages = make([]*storage.LevelDBStorage, 0)
	return nil
}
