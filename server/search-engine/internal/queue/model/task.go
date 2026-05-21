package model

import "campusmart/search-engine/internal/searcher/model"

type TaggingTask struct {
	Database string          `json:"database"`
	Doc      *model.IndexDoc `json:"doc"`
}

type IndexTask struct {
	Database string          `json:"database"`
	Doc      *model.IndexDoc `json:"doc"`
	Source   string          `json:"source"`
}
