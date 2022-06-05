package opa_server_config

import (
	"OpaServer/pkg/utils"
	"database/sql"
	"fmt"
	"github.com/bluele/gcache"
	"gopkg.in/yaml.v2"
	"io/ioutil"
	"path/filepath"
)

func PrepareServerSetting(context *OPAServerContext) error {
	context.FuncTimeCounter = make(map[string]int64)
	err := parseConfig(context)
	if err != nil {
		fmt.Printf("fail to parse config file, err: %v\n", err)
		return err
	}

	PrepareRego(context)

	return nil
}

type ApplicationConfig struct {
	FuncCache struct {
		FuncUseCache      bool  `yaml:"func_use_cache"`
		FuncCacheTTL      int64 `yaml:"func_cache_ttl"`
		FuncMaxCacheEntry int   `yaml:"func_max_cache_entry"`
	} `yaml:"func_cache"`
	HieCache struct{
		HieUseCache      bool  `yaml:"hie_use_cache"`
		HieCacheTTL      int64 `yaml:"hie_cache_ttl"`
		HieMaxCacheEntry int   `yaml:"hie_max_cache_entry"`
	} `yaml:"hie_cache"`
	Datasource []struct {
		DriveName string `yaml:"drive_name"`
		Name      string `yaml:"name"`
		URL       string `yaml:"url"`
	} `yaml:"datasource"`
}

// parseConfig used to parse the application.yml configuration file
func parseConfig(context *OPAServerContext) error {
	rootPath := utils.GetProjectRoot()
	context.ProjectRootPath = rootPath
	configFilePath := filepath.Join(rootPath, "resources/application.yml")
	context.ConfigFilePath = configFilePath
	var applicationConfig ApplicationConfig
	configContent, err := ioutil.ReadFile(configFilePath)
	if err != nil {
		fmt.Printf("fail to rady config file: %v\n", configFilePath)
		return err
	}
	err = yaml.Unmarshal(configContent, &applicationConfig)
	if err != nil {
		fmt.Printf("fail to parse config file: %v\n", configFilePath)
		return err
	}
	//fmt.Printf("config: %v\n", applicationConfig)
	if applicationConfig.FuncCache.FuncUseCache {
		context.FuncUseCache = true
		context.FuncCacheTTL = applicationConfig.FuncCache.FuncCacheTTL
		context.MaxFuncCacheEntry = applicationConfig.FuncCache.FuncMaxCacheEntry
		context.FuncCache = gcache.New(context.MaxFuncCacheEntry).LFU().Build()
	} else {
		context.FuncUseCache = false
	}

	if applicationConfig.HieCache.HieUseCache {
		context.HieUseCache = true
		context.HieCacheTTL = applicationConfig.HieCache.HieCacheTTL
		context.MaxHieCacheEntry = applicationConfig.HieCache.HieMaxCacheEntry
		context.HierarchyCache = gcache.New(context.MaxHieCacheEntry).LFU().Build()
	}else {
		context.HieUseCache = false
	}

	if context.SqlDB == nil {
		context.SqlDB = make(map[string]*sql.DB)
	}

	for _, dataConfig := range applicationConfig.Datasource {
		if dataConfig.DriveName == utils.MYSQL {
			conn, err := setupSQLDatabase(dataConfig.DriveName, dataConfig.URL)
			if err != nil {
				fmt.Printf("fail to set up database connection, driver name: %v, url: %v\n", dataConfig.DriveName,
					dataConfig.URL)
				return err
			}
			context.SqlDB[dataConfig.Name] = conn
		}
	}

	//fmt.Printf("complete context: %v\n", context)

	return nil
}

func setupSQLDatabase(driverName string, connString string) (*sql.DB, error) {
	// change "postgres" for whatever supported database you want to use
	db, err := sql.Open(driverName, connString)

	if err != nil {
		return nil, err
	}

	// ping the DB to ensure that it is connected
	err = db.Ping()

	if err != nil {
		return nil, err
	}

	return db, nil
}

func (context *OPAServerContext) Close() {
	for _, conn := range context.SqlDB {
		count := 1
		err := conn.Close()
		for count < 5 && err != nil {
			count++
			err = conn.Close()
		}
	}
}
