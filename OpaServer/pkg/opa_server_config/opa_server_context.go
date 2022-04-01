package opa_server_config

import (
	"database/sql"
	"github.com/bluele/gcache"
)

type OPAServerContext struct {
	SqlDB           map[string]*sql.DB
	UseCache        bool
	FuncCache       gcache.Cache
	FuncCacheTTL    int64
	MaxCacheEntry   int
	ProjectRootPath string
	ConfigFilePath  string
	FuncTimeCounter map[string]int64
}
