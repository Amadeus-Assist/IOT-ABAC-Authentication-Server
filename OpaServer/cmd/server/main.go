package main

import (
	"OpaServer/pkg/api"
	"OpaServer/pkg/app"
	"OpaServer/pkg/opa_server_config"
	"fmt"
	"github.com/gin-contrib/cors"
	"github.com/gin-gonic/gin"
	_ "github.com/go-sql-driver/mysql"
	"os"
)

func main() {
	if err := run(); err != nil {
		_, err := fmt.Fprintf(os.Stderr, "this is the startup error: %s\\n", err)
		if err != nil {
			return
		}
		os.Exit(1)
	}
}

type memberRef struct {
	ref string
}

// func run will be responsible for setting up db connections, routers etc
func run() error {

	var context opa_server_config.OPAServerContext

	// prepare the context for server
	if err := opa_server_config.PrepareServerSetting(&context); err != nil {
		fmt.Printf("fail to set up OPA server config, err: %v\n", err)
		return err
	}

	defer context.Close()

	router := gin.Default()
	router.Use(cors.Default())

	opaEvalService := api.NewOpaEvalService()

	server := app.NewServer(router, opaEvalService, &context)

	err := server.Run()

	if err != nil {
		return err
	}

	return nil
}
