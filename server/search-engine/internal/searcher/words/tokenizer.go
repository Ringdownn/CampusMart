package words

import (
	"campusmart/search-engine/internal/searcher/translator"
	"campusmart/search-engine/internal/searcher/utils"
	"embed"
	"strings"

	"github.com/wangbin/jiebago"
)

var (
	//go:embed data/*.txt
	dictionaryFS embed.FS
)

type Tokenizer struct {
	seg jiebago.Segmenter
}

func NewTokenizer(dictionaryPath string) *Tokenizer {
	file, err := dictionaryFS.Open("data/dictionary.txt")
	if err != nil {
		panic(err)
	}
	utils.ReleaseAssets(file, dictionaryPath)
	tokenizer := new(Tokenizer)
	err = tokenizer.seg.LoadDictionary(dictionaryPath)
	if err != nil {
		panic(err)
	}
	return tokenizer
}

func (t *Tokenizer) Cut(text string) []string {
	//全部小写
	text = strings.ToLower(text)
	//去除标点符号
	text = utils.RemovePunctuation(text)

	//提取英文单词
	var wordsMap = make(map[string]struct{})
	var wordsSlice []string

	englishWords := utils.ExtractEnglishWords(text)
	for _, word := range englishWords {
		_, find := wordsMap[word]
		if !find {
			wordsSlice = append(wordsSlice, word)
			wordsMap[word] = struct{}{}
		}
	}

	numberWords := utils.ExtractNumberWords(text)
	for _, word := range numberWords {
		_, find := wordsMap[word]
		if !find {
			wordsSlice = append(wordsSlice, word)
			wordsMap[word] = struct{}{}
		}
	}

	//去除英文
	text = utils.RemoveEnglish(text)

	//去除空格
	text = utils.RemoveSpace(text)

	//开启分词，返回一个channel
	resultChan := t.seg.CutForSearch(text, true)
	for {
		//从channel中取出词
		w, ok := <-resultChan
		if !ok {
			break
		}
		_, found := wordsMap[w]
		if !found {
			//标记已经存在并加入到结果
			wordsMap[w] = struct{}{}
			wordsSlice = append(wordsSlice, w)
		}

	}
	return wordsSlice
}

func (t *Tokenizer) CutWithTranslate(text string) []string {
	//全部小写
	text = strings.ToLower(text)
	//去除标点符号
	text = utils.RemovePunctuation(text)

	//提取英文单词
	var wordsMap = make(map[string]struct{})
	var wordsSlice []string

	englishWords := utils.ExtractEnglishWords(text)
	for _, word := range englishWords {
		_, find := wordsMap[word]
		if !find {
			wordsSlice = append(wordsSlice, word)
			wordsMap[word] = struct{}{}
		}
	}

	numberWords := utils.ExtractNumberWords(text)
	for _, word := range numberWords {
		_, find := wordsMap[word]
		if !find {
			wordsSlice = append(wordsSlice, word)
			wordsMap[word] = struct{}{}
		}
	}

	//去除英文
	text = utils.RemoveEnglish(text)

	//去除空格
	text = utils.RemoveSpace(text)

	var chineseWordSlice = make([]string, 0)

	//开启分词，返回一个channel
	resultChan := t.seg.CutForSearch(text, true)
	for {
		//从channel中取出词
		w, ok := <-resultChan
		if !ok {
			break
		}
		_, found := wordsMap[w]
		if !found {
			//标记已经存在并加入到结果
			wordsMap[w] = struct{}{}
			wordsSlice = append(wordsSlice, w)
			chineseWordSlice = append(chineseWordSlice, w)
		}
	}

	//放入翻译后的单词
	translatedWords := translator.TranslateWordsToEn(chineseWordSlice)
	translatedWords = append(translatedWords, translator.TranslateWordsToCn(englishWords)...)
	//fmt.Print(translatedWords)

	for _, word := range translatedWords {
		_, find := wordsMap[word]
		if !find {
			wordsSlice = append(wordsSlice, word)
			wordsMap[word] = struct{}{}
		}
	}

	return wordsSlice
}
