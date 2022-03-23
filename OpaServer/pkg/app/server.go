package app

import (
	"OpaServer/pkg/api"
	"github.com/gin-gonic/gin"
	"log"
)

type Server struct {
	router         *gin.Engine
	opaEvalService api.OpaEvalService
}

func NewServer(router *gin.Engine, opaEvalService api.OpaEvalService) *Server {
	return &Server{router: router, opaEvalService: opaEvalService}
}

func (s *Server) Run() error {
	r := s.Routes()

	err := r.Run()

	if err != nil {
		log.Printf("server - there was an error calling Run on router: %v\n", err)
		return err
	}

	return nil
}
