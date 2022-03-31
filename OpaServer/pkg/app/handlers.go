package app

import (
	"OpaServer/pkg/api"
	"fmt"
	"github.com/gin-gonic/gin"
	"log"
	"net/http"
)

func (s *Server) Eval() gin.HandlerFunc {
	return func(context *gin.Context) {
		context.Header("Content-Type", "application/json")

		var newEvalRequest api.OpaEvalRequest

		//bodyJson, err := ioutil.ReadAll(context.Request.Body);

		//if err != nil {
		//	log.Printf("err: %v\n", err)
		//	context.JSON(http.StatusBadRequest, nil)
		//	return
		//}

		//fmt.Printf("Request: %v\n", bodyJson)

		err := context.ShouldBindJSON(&newEvalRequest)

		fmt.Printf("request:\naccess_request: %v\npolicy: %v\n", newEvalRequest.AccessRequest, newEvalRequest.Policy)

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
