package opa_server_config

import (
	"database/sql"
	"github.com/bluele/gcache"
)

type OPAServerContext struct {
	SqlDB         *sql.DB
	FuncCache     gcache.Cache
	FuncCacheTTL  int64
	MaxCacheEntry int
}
