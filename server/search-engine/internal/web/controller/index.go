package controller

import (
	"campusmart/search-engine/internal/searcher/model"

	"github.com/gin-gonic/gin"
)

func AddIndex(c *gin.Context) {
	document := &model.IndexDoc{}
	if err := c.ShouldBindJSON(document); err != nil {
		ResponseErrorWithMessage(c, err.Error())
		return
	}
	dbName := c.Query("database")
	if dbName == "" {
		ResponseErrorWithMessage(c, "Database is empty")
		return
	}
	err := srv.Index.AddIndex(dbName, document)
	if err != nil {
		ResponseErrorWithMessage(c, err.Error())
		return
	}
	ResponseSuccess(c)
}

func BatchAddIndex(c *gin.Context) {
	documents := make([]*model.IndexDoc, 0)
	if err := c.BindJSON(&documents); err != nil {
		ResponseErrorWithMessage(c, err.Error())
		return
	}

	dbName := c.Query("database")
	if dbName == "" {
		ResponseErrorWithMessage(c, "Database is empty")
		return
	}

	err := srv.Index.BatchAddIndex(dbName, documents)
	if err != nil {
		ResponseErrorWithMessage(c, err.Error())
		return
	}
	ResponseSuccess(c)
}

func RemoveIndex(c *gin.Context) {
	document := &model.RemoveIndexModel{}
	if err := c.BindJSON(document); err != nil {
		ResponseErrorWithMessage(c, err.Error())
		return
	}

	dbName := c.Query("database")
	if dbName == "" {
		ResponseErrorWithMessage(c, "Database is empty")
		return
	}

	if err := srv.Index.RemoveIndex(dbName, document); err != nil {
		ResponseErrorWithMessage(c, err.Error())
		return
	}
	ResponseSuccess(c)
}
