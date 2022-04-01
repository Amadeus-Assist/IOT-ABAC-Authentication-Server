package app

import (
	"OpaServer/pkg/api"
	"OpaServer/pkg/opa_server_config"
	"github.com/gin-gonic/gin"
	"log"
)

type Server struct {
	router         *gin.Engine
	opaEvalService api.OpaEvalService
	context        *opa_server_config.OPAServerContext
}

func NewServer(router *gin.Engine, opaEvalService api.OpaEvalService, context *opa_server_config.OPAServerContext) *Server {
	return &Server{router: router, opaEvalService: opaEvalService, context: context}
}

func (s *Server) Run() error {
	r := s.Routes()

	err := r.Run(":8081")

	if err != nil {
		log.Printf("server - there was an error calling Run on router: %v\n", err)
		return err
	}

	return nil
}
