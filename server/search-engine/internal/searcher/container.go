package searcher

import (
	"campusmart/search-engine/internal/searcher/words"
	"errors"
	"fmt"
	"log"
	"os"
	"runtime"
)

type Container struct {
	Dir       string
	engines   map[string]*Engine
	Debug     bool
	Tokenizer *words.Tokenizer
	Shard     int
	Timeout   int64
	BufferNum int
}

func (c *Container) Init() error {
	c.engines = make(map[string]*Engine)

	//读取当前目录下所有目录，就是数据库名称
	dirs, err := os.ReadDir(c.Dir)
	if err != nil {
		if os.IsNotExist(err) {
			//创建目录
			err := os.MkdirAll(c.Dir, os.ModePerm)
			if err != nil {
				return err
			}
		} else {
			return err
		}
	}
	//初始化数据库
	for _, dir := range dirs {
		if dir.IsDir() {
			c.engines[dir.Name()] = c.GetDataBase(dir.Name())
			log.Print("db:", dir.Name())
		}
	}

	return nil
}

// 创建一个引擎
func (c *Container) NewEngine(name string) *Engine {
	var engine = &Engine{
		IndexPath:    fmt.Sprintf("%s%c%s", c.Dir, os.PathSeparator, name),
		DatabaseName: name,
		Tokenizer:    c.Tokenizer,
		Timeout:      c.Timeout,
		BufferNum:    c.BufferNum,
		Shard:        c.Shard,
	}
	option := engine.GetOptions()

	engine.InitOption(option)
	engine.IsDebug = c.Debug

	return engine
}

// GetDataBase 获取或创建引擎
func (c *Container) GetDataBase(name string) *Engine {
	if name == "" {
		name = "default"
	}

	engine, ok := c.engines[name]
	if !ok {
		engine = c.NewEngine(name)
		c.engines[name] = engine
	}

	return engine
}

func (c *Container) GetDataBases() map[string]*Engine {
	return c.engines
}

func (c *Container) GetDataBaseNumber() int {
	return len(c.engines)
}

func (c *Container) GetIndexCount() int64 {
	var count int64
	for _, engine := range c.engines {
		count += engine.GetIndexCount()
	}
	return count
}

func (c *Container) GetDocumentCount() int64 {
	var count int64
	for _, engine := range c.engines {
		count += engine.GetDocumentCount()
	}
	return count
}

func (c *Container) DropDatabase(name string) error {
	if _, ok := c.engines[name]; !ok {
		return errors.New(fmt.Sprintf("database %s not exist", name))
	}
	err := c.engines[name].Drop()
	if err != nil {
		return err
	}

	delete(c.engines, name)
	runtime.GC()

	return nil
}
