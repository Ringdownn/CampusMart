package service

import (
	"campusmart/search-engine/internal/global"
	"campusmart/search-engine/internal/searcher"
	"campusmart/search-engine/internal/searcher/model"
)

type Base struct {
	Container *searcher.Container
}

func NewBase() *Base {
	return &Base{
		Container: global.Container,
	}
}

func (b *Base) Query(request *model.SearchRequest) (*model.SearchResult, error) {
	return b.Container.GetDataBase(request.Database).MultiSearch(request)
}
