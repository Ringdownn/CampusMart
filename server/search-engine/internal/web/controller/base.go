package controller

import (
	"campusmart/search-engine/internal/searcher/model"

	"github.com/gin-gonic/gin"
)

func Query(c *gin.Context) {
	var request = &model.SearchRequest{
		Database: c.Query("database"),
	}
	if err := c.ShouldBind(&request); err != nil {
		ResponseErrorWithMessage(c, err.Error())
		return
	}
	//调用搜索
	r, err := srv.Base.Query(request)
	if err != nil {
		ResponseErrorWithMessage(c, err.Error())
	} else {
		ResponseSuccessWithData(c, r)
	}
}
