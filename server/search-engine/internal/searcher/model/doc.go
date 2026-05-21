package model

// 索引储存
type IndexDoc struct {
	Id       uint32                 `json:"id,omitempty"`
	Text     string                 `json:"text,omitempty"`
	ImageURL string                 `json:"imageURL,omitempty"`
	Tags     []string               `json:"tags,omitempty"`
	Document map[string]interface{} `json:"document,omitempty"`
}

// 索引文件存储
type StorageIndexDoc struct {
	*IndexDoc
	Keys []string `json:"keys,omitempty"` //分词
}

type ResponseDoc struct {
	IndexDoc
	OriginalText string   `json:"originalText,omitempty"`
	Score        int      `json:"score,omitempty"` //得分
	Keys         []string `json:"keys,omitempty"`  //分词
}

type RemoveIndexModel struct {
	Id uint32 `json:"id,omitempty"`
}

type ResponseDocsSort []ResponseDoc

func (r ResponseDocsSort) Len() int {
	return len(r)
}

func (r ResponseDocsSort) Less(i, j int) bool {
	return r[i].Score < r[j].Score
}

func (r ResponseDocsSort) Swap(i, j int) {
	r[i], r[j] = r[j], r[i]
}
