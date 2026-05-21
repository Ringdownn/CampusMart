package core

import (
	"campusmart/search-engine/internal/global"
	"campusmart/search-engine/internal/searcher"
	"campusmart/search-engine/internal/searcher/words"
)

// Initialize 只负责初始化核心组件，不启动服务
func Initialize() {
	// 加载配置
	global.CONFIG = Parse()

	// 加载词典，初始化分词器
	tokenizer := NewTokenizer(global.CONFIG.Dictionary)

	// 初始化容器
	global.Container = NewContainer(tokenizer)
}

// NewContainer 初始化容器
func NewContainer(tokenizer *words.Tokenizer) *searcher.Container {
	container := &searcher.Container{
		Dir:       global.CONFIG.Data,
		Debug:     global.CONFIG.Debug,
		Shard:     global.CONFIG.Shard,
		Timeout:   global.CONFIG.Timeout,
		BufferNum: global.CONFIG.BufferNum,
		Tokenizer: tokenizer,
	}
	if err := container.Init(); err != nil {
		panic(err)
	}

	return container
}

// NewTokenizer 初始化分词器
func NewTokenizer(dictionaryPath string) *words.Tokenizer {
	return words.NewTokenizer(dictionaryPath)
}
