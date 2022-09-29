package app

import "github.com/gin-gonic/gin"

func (s *Server) Routes() *gin.Engine {
	router := s.router

	// group all routes under /v1/api
	v4 := router.Group("/reorder_test")
	{
		v4.GET("/:count", s.ReorderTest())
	}

	return router
}
