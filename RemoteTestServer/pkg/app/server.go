package app

import (
	"database/sql"
	"github.com/gin-gonic/gin"
	"log"
)

type Server struct {
	router *gin.Engine
	conn   *sql.DB
}

func NewServer(router *gin.Engine, conn *sql.DB) *Server {
	return &Server{router: router, conn: conn}
}

func (s *Server) Run() error {
	r := s.Routes()

	err := r.Run(":8082")

	if err != nil {
		log.Printf("server - there was an error calling Run on router: %v\n", err)
		return err
	}

	return nil
}
