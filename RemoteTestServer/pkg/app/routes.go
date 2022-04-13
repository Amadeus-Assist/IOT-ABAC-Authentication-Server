package app

import "github.com/gin-gonic/gin"

func (s *Server) Routes() *gin.Engine {
	router := s.router

	// group all routes under /v1/api
	v1 := router.Group("/columbia")
	{
		v1.GET("/:id", s.Query())
	}

	return router
}
