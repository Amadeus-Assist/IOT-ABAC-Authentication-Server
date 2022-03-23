package app

import "github.com/gin-gonic/gin"

func (s *Server) Routes() *gin.Engine {
	router := s.router

	// group all routes under /v1/api
	v1 := router.Group("/opa")
	{
		v1.GET("/eval", s.Eval())
	}

	return router
}
