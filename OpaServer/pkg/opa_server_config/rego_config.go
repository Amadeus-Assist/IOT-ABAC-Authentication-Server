package opa_server_config

import (
	"OpaServer/pkg/utils"
	"database/sql"
	"fmt"
	"github.com/bluele/gcache"
	"github.com/open-policy-agent/opa/ast"
	"github.com/open-policy-agent/opa/rego"
	"github.com/open-policy-agent/opa/types"
	"io/ioutil"
	"net/http"
	"strings"
	"time"
)

type queriedAttributes struct {
	attrs string
}

// PrepareRego defines sql_query and api_get_obj_term function
func PrepareRego(context *OPAServerContext) {
	rego.RegisterBuiltin3(&rego.Function{
		Name: "sql_query",
		Decl: types.NewFunction(
			types.Args(types.S, types.S, types.NewArray(make([]types.Type, 0), types.S)),
			types.A,
		),
	}, func(_ rego.BuiltinContext, datasourceTerm, tempTerm, paramsTerm *ast.Term) (*ast.Term, error) {
		start := time.Now().UnixMicro()
		// start timer to count its cost
		var funcArgs = []*ast.Term{tempTerm, paramsTerm}
		cacheKey := buildCacheKey("sql_query", funcArgs)
		if context.FuncUseCache {
			// if use function cache, check cache first
			cacheRes, prs := checkCache(context.FuncCache, cacheKey)
			if prs {
				return &cacheRes, nil
			}
		}

		datasource, ok1 := datasourceTerm.Value.(ast.String)
		temp, ok2 := tempTerm.Value.(ast.String)
		params, ok3 := paramsTerm.Value.(*ast.Array)

		if !ok1 || !ok2 || !ok3 {
			fmt.Printf("convert param err\n")
			return nil, nil
		}

		paramsSlice := make([]any, params.Len())
		for i := range paramsSlice {
			paramsSlice[i] = string(params.Elem(i).Value.(ast.String))
		}

		sqlConn, prs := context.SqlDB[string(datasource)]
		if !prs {
			fmt.Printf("unable to find corresponding datasource: %v\n", datasource)
			return nil, nil
		}

		res, err := sqlConn.Query(string(temp), paramsSlice...)
		//fmt.Printf("query temp: %v, params: %v\n", temp, paramsSlice)

		if err != nil {
			fmt.Printf("Unable to execute sql_query, template: %v, params: %v, err: %v\n",
				temp.String(), paramsSlice, err)
			return nil, nil
		}

		defer func(res *sql.Rows) {
			err := res.Close()
			if err != nil {
				fmt.Printf("close res err: %v\n", err)
			}
		}(res)

		var attributes queriedAttributes

		if res.Next() {
			if err := res.Scan(&attributes.attrs); err != nil {
				fmt.Printf("scan err: %v\n", err)
				return nil, nil
			}
		} else {
			fmt.Printf("empty query result, temp: %v, param: %v\n", string(temp), paramsSlice[0])
			return nil, nil
		}

		fmt.Printf("sql query result: %v\n", attributes.attrs)

		astTerm, err := ast.ParseTerm(attributes.attrs)

		if err != nil {
			fmt.Printf("Parse term error, source: %v, err: %v\n", attributes.attrs, err)
			return nil, nil
		}

		if context.FuncUseCache {
			// if used function cache, push to result to cache
			err = pushToCache(context.FuncCache, cacheKey, astTerm, context.FuncCacheTTL)
			if err != nil {
				fmt.Printf("push to cache error: %v\n", err)
			}
		}

		// calculate function cost then use EWMA calculate avg
		funcKey := "sql_query(" + string(datasource) + ")"
		elapse := time.Now().UnixMicro() - start
		oldElapse, prsE := context.FuncTimeCounter[funcKey]
		if !prsE {
			context.FuncTimeCounter[funcKey] = elapse
		} else {
			rate := utils.EWMA_RATE
			newElapse := int64(float64(oldElapse)*(1-rate) + rate*float64(elapse))
			context.FuncTimeCounter[funcKey] = newElapse
		}

		fmt.Printf("counter:\n%v\n\n", context.FuncTimeCounter)

		return astTerm, nil
	})

	rego.RegisterBuiltin2(&rego.Function{
		Name: "api_get_obj_term",
		Decl: types.NewFunction(
			types.Args(types.S, types.NewArray(make([]types.Type, 0), types.S)),
			types.A,
		),
	}, func(_ rego.BuiltinContext, urlPrefixTerm, paramsTerm *ast.Term) (*ast.Term, error) {
		start := time.Now().UnixMicro()

		urlPrefix, ok1 := urlPrefixTerm.Value.(ast.String)
		params, ok2 := paramsTerm.Value.(*ast.Array)
		if !ok1 || !ok2 {
			fmt.Printf("convert param err\n")
			return nil, nil
		}

		var funcArgs = []*ast.Term{paramsTerm}
		cacheKey := buildCacheKey("api_get_obj_term", funcArgs)

		if context.FuncUseCache {
			cacheRes, prs := checkCache(context.FuncCache, cacheKey)
			if prs {
				return &cacheRes, nil
			}
		}

		var sb strings.Builder

		sb.WriteString(string(urlPrefix))
		sb.WriteString("/")

		params.Foreach(func(term *ast.Term) {
			sb.WriteString(string(term.Value.(ast.String)))
			sb.WriteString("/")
		})

		tmpUrl := sb.String()

		finalUrl := tmpUrl[:len(tmpUrl)-1]

		resp, err := http.Get(finalUrl)
		if err != nil {
			fmt.Printf("api_get_obj_term, get fail: %v\n", err)
			return nil, nil
		}

		body, err := ioutil.ReadAll(resp.Body)
		if err != nil {
			fmt.Printf("api_get_obj_term, invalid response: %v\n", err)
			return nil, nil
		}

		responseContent := string(body)

		//fmt.Printf("result: %v\n", responseContent)

		astTerm, err := ast.ParseTerm(responseContent)
		if err != nil {
			fmt.Printf("Parse term error, source: %v, err: %v\n", responseContent, err)
			return nil, nil
		}
		//fmt.Printf("res term: %v\n", astTerm)

		if context.FuncUseCache {
			err = pushToCache(context.FuncCache, cacheKey, astTerm, context.FuncCacheTTL)
			if err != nil {
				fmt.Printf("push to cache error: %v\n", err)
			}
		}

		funcKey := "api_get_obj_term(" + string(urlPrefix) + ")"
		elapse := time.Now().UnixMicro() - start
		oldElapse, prsE := context.FuncTimeCounter[funcKey]
		if !prsE {
			context.FuncTimeCounter[funcKey] = elapse
		} else {
			rate := utils.EWMA_RATE
			newElapse := int64(float64(oldElapse)*(1-rate) + rate*float64(elapse))
			context.FuncTimeCounter[funcKey] = newElapse
		}

		fmt.Printf("counter:\n%v\n\n", context.FuncTimeCounter)

		return astTerm, nil
	})
}

func checkCache(cache gcache.Cache, cacheKey string) (ast.Term, bool) {
	value, err := cache.GetIFPresent(cacheKey)
	if err != nil {
		return ast.Term{}, false
	}
	term, ok := value.(ast.Term)
	if !ok {
		return ast.Term{}, false
	}
	fmt.Printf("hit cache!\n")
	return term, true
}

func buildCacheKey(funcName string, args []*ast.Term) string {
	var sb strings.Builder
	sb.WriteString(funcName)
	sb.WriteString("(")
	for _, arg := range args {
		sb.WriteString(arg.String())
		sb.WriteString(",")
	}
	sb.WriteString(")")
	return sb.String()
}

func pushToCache(cache gcache.Cache, key string, value *ast.Term, expireTime int64) error {
	err := cache.SetWithExpire(key, *value, time.Duration(expireTime)*time.Second)
	if err != nil {
		fmt.Printf("set lfu cache error: %v\n", err)
		return err
	}
	return nil
}
