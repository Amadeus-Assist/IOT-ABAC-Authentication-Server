package app

import (
	"database/sql"
	"log"

	"github.com/gin-gonic/gin"
)

type Mapkey struct {
	User_id    string
	Table_name string
}
type Server struct {
	router     *gin.Engine
	conn       *sql.DB
	allow_once map[Mapkey]bool
}

func NewServer(router *gin.Engine, conn *sql.DB) *Server {
	return &Server{router: router, conn: conn}
}

func (s *Server) Run() error {
	r := s.Routes()

	err := r.Run(":3333")

	if err != nil {
		log.Printf("server - there was an error calling Run on router: %v\n", err)
		return err
	}

	return nil
}
