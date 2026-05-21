package model

type SearchRequest struct {
	Query    string `json:"query,omitempty" form:"query"`
	Order    string `json:"order,omitempty" form:"order"`
	ScoreExp string `json:"scoreExp,omitempty" form:"scoreExp"`
	Page     int    `json:"page,omitempty" form:"page"`
	Limit    int    `json:"limit,omitempty" form:"limit"`
	Database string `json:"database,omitempty" form:"database"`
}

func (r *SearchRequest) GetAndSetDefault() *SearchRequest {
	if r.Limit == 0 {
		r.Limit = 100
	}
	if r.Page == 0 {
		r.Page = 1
	}
	if r.Order == "" {
		r.Order = "desc"
	}
	return r
}

type SearchResult struct {
	Time      float64       `json:"time,omitempty"`
	Total     int           `json:"total"`
	PageCount int           `json:"pageCount"`
	Page      int           `json:"page,omitempty"`
	Limit     int           `json:"limit,omitempty"`
	Documents []ResponseDoc `json:"documents,omitempty"`
	Words     []string      `json:"words,omitempty"`
}
