package app

import (
	"OpaServer/pkg/api"
	"github.com/gin-gonic/gin"
	"log"
	"net/http"
)

func (s *Server) Eval() gin.HandlerFunc {
	return func(context *gin.Context) {
		context.Header("Content-Type", "application/json")

		var newEvalRequest api.OpaEvalRequest

		err := context.ShouldBindJSON(&newEvalRequest)

		if err != nil {
			log.Printf("handler error: %v\n", err)
			context.JSON(http.StatusBadRequest, nil)
			return
		}

		decision, err := s.opaEvalService.Eval(&newEvalRequest)

		if err != nil {
			log.Printf("eval request error: %v\n", err)
			context.JSON(http.StatusBadRequest, nil)
			return
		}

		response := map[string]string{
			"decision": decision,
		}

		context.JSON(http.StatusOK, response)
	}
}
