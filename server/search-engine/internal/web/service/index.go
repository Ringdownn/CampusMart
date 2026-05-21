package service

import (
	"campusmart/search-engine/internal/global"
	"campusmart/search-engine/internal/queue/producer"
	"campusmart/search-engine/internal/searcher"
	"campusmart/search-engine/internal/searcher/model"
	"log"
)

type Index struct {
	Container       *searcher.Container
	taggingProducer *producer.TaggingProducer
}

// NewIndex 初始化 Index 服务，注入 taggingProducer
func NewIndex(taggingProducer *producer.TaggingProducer) *Index {
	return &Index{
		Container:       global.Container,
		taggingProducer: taggingProducer,
	}
}

func (index *Index) AddIndex(dbName string, doc *model.IndexDoc) error {
	if doc.ImageURL != "" {
		if err := index.taggingProducer.PublishTaggingTask(dbName, doc); err != nil {
			log.Printf("发送到打标队列失败,docId: %d, error: %v", doc.Id, err)
			return index.Container.GetDataBase(dbName).IndexDocument(doc)
		}
	}
	return index.Container.GetDataBase(dbName).IndexDocument(doc)
}

func (index *Index) RemoveIndex(dbName string, doc *model.RemoveIndexModel) error {
	db := index.Container.GetDataBase(dbName)
	if err := db.RemoveIndex(doc.Id); err != nil {
		return err
	}
	return nil
}

func (index *Index) BatchAddIndex(dbName string, docs []*model.IndexDoc) error {
	c := index.Container.GetDataBase(dbName)
	var failedDocs []struct {
		Doc   *model.IndexDoc
		Error error
	}

	for _, doc := range docs {
		var err error
		if doc.ImageURL != "" {
			err = index.taggingProducer.PublishTaggingTask(dbName, doc)
			if err != nil {
				log.Printf("发送到打标队列失败, docId: %d, error: %v", doc.Id, err)
				failedDocs = append(failedDocs, struct {
					Doc   *model.IndexDoc
					Error error
				}{doc, err})
				continue
			}
			err = c.IndexDocument(doc)
			if err != nil {
				failedDocs = append(failedDocs, struct {
					Doc   *model.IndexDoc
					Error error
				}{doc, err})
				continue
			}
		} else {
			err = c.IndexDocument(doc)
			if err != nil {
				failedDocs = append(failedDocs, struct {
					Doc   *model.IndexDoc
					Error error
				}{doc, err})
				continue
			}
		}
	}

	if len(failedDocs) > 0 {
		log.Printf("BatchAddIndex: %d/%d 文档处理失败", len(failedDocs), len(docs))
	}

	return nil
}
