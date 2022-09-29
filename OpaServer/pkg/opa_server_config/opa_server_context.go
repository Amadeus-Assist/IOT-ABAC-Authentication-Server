package opa_server_config

import (
	"database/sql"
	"github.com/bluele/gcache"
)

type OPAServerContext struct {
	// context for server
	SqlDB             map[string]*sql.DB
	FuncUseCache      bool
	FuncCache         gcache.Cache
	FuncCacheTTL      int64
	MaxFuncCacheEntry int
	ProjectRootPath   string
	ConfigFilePath    string
	FuncTimeCounter   map[string]int64
	HieUseCache       bool
	HierarchyCache    gcache.Cache
	MaxHieCacheEntry  int
	HieCacheTTL       int64
	RuleReorder       bool
}
