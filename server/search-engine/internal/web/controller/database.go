package controller

import "github.com/gin-gonic/gin"

func DBS(c *gin.Context) {
	ResponseSuccessWithData(c, srv.Database.Show())
}

func DatabaseCreate(c *gin.Context) {
	dbName := c.Query("database")
	if dbName == "" {
		ResponseErrorWithMessage(c, "database is empty")
		return
	}

	srv.Database.Create(dbName)
	ResponseSuccessWithData(c, "create success")
}

func DatabaseDrop(c *gin.Context) {
	dbName := c.Query("database")
	if dbName == "" {
		ResponseErrorWithMessage(c, "database is empty")
		return
	}

	err := srv.Database.Drop(dbName)
	if err != nil {
		ResponseErrorWithMessage(c, err.Error())
		return
	}
	ResponseSuccessWithData(c, "drop success")
}
