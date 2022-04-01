package test

import (
	"OpaServer/pkg/utils"
	"fmt"
	"github.com/bluele/gcache"
	"io/ioutil"
	"path/filepath"
	"regexp"
	"testing"
	"time"
	"gopkg.in/yaml.v2"
)

func TestLFU(t *testing.T) {
	lfu := gcache.New(20).LFU().Build()
	lfu.SetWithExpire("key", "ok", 1*time.Second)
	value, err := lfu.GetIFPresent("key")
	if err != nil {
		fmt.Printf("error: %v\n", err)
	} else {
		fmt.Printf("value: %v\n", value)
	}

	time.Sleep(1 * time.Second)

	value, err = lfu.GetIFPresent("key")
	if err != nil {
		fmt.Printf("error: %v\n", err)
	} else {
		fmt.Printf("value: %v\n", value)
	}
}

func TestParseYaml(t *testing.T) {
	type OpaConfigCache struct {
		UseCache      bool  `yaml:"use_cache"`
		FuncCacheTTL  int64 `yaml:"func_cache_ttl"`
		MaxCacheEntry int   `yaml:"max_cache_entry"`
	}

	type OpaConfigDatasource struct {
		Type string `yaml:"type"`
		Name string `yaml:"name"`
		URL  string `yaml:"url"`
	}

	type OpaConfig struct {
		Cache      OpaConfigCache        `yaml:"cache"`
		Datasource []OpaConfigDatasource `yaml:"datasource,flow"`
	}

	var opaConfig OpaConfig

	projectRootPath := utils.GetProjectRoot()

	fmt.Printf("root path: %v\n", projectRootPath)

	configFilePath := filepath.Join(projectRootPath, "resources/application.yml")

	yamContent, err := ioutil.ReadFile(configFilePath)
	if err != nil {
		panic(err)
	}
	err = yaml.Unmarshal(yamContent, &opaConfig)
	if err != nil {
		panic(err)
	}

	fmt.Printf("config: %v\n", opaConfig)
}

func TestRegex(t *testing.T) {
	s := `sub_owner_attr := sql_query("abac", "SELECT attrs FROM user_attrs WHERE user_id = ?", [input.sub.owner_id])`
	match, _ := regexp.MatchString(utils.SQL_QUERY_TERM_REGEX, s)
	fmt.Printf("res: %v\n", match)
}
