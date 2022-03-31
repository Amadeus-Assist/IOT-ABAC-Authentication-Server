package opa_server_config

import (
	"database/sql"
	"fmt"
	"github.com/bluele/gcache"
	"github.com/open-policy-agent/opa/ast"
	"github.com/open-policy-agent/opa/rego"
	"github.com/open-policy-agent/opa/types"
	"strings"
	"time"
)

type queriedAttributes struct {
	attrs string
}

func PrepareRego(context OPAServerContext) {
	context.FuncCacheTTL = 3600 * 12
	context.MaxCacheEntry = 10000
	context.FuncCache = gcache.New(context.MaxCacheEntry).LFU().Build()
	rego.RegisterBuiltin2(&rego.Function{
		Name: "sql_query",
		Decl: types.NewFunction(
			types.Args(types.S, types.NewArray(make([]types.Type, 0), types.S)),
			types.A,
		),
		Memoize: true,
	}, func(_ rego.BuiltinContext, tempTerm, paramsTerm *ast.Term) (*ast.Term, error) {
		var funcArgs = []*ast.Term{tempTerm, paramsTerm}
		cacheKey := buildCacheKey("sql_query", funcArgs)
		cacheRes, prs := checkCache(context.FuncCache, cacheKey)
		if prs {
			return &cacheRes, nil
		}

		temp, ok1 := tempTerm.Value.(ast.String)
		params, ok2 := paramsTerm.Value.(*ast.Array)

		if !ok1 || !ok2 {
			fmt.Printf("convert param err\n")
			return nil, nil
		}

		paramsSlice := make([]any, params.Len())
		for i := range paramsSlice {
			paramsSlice[i] = string(params.Elem(i).Value.(ast.String))
		}

		res, err := context.SqlDB.Query(string(temp), paramsSlice...)
		fmt.Printf("query temp: %v, params: %v\n", temp, paramsSlice)

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
			fmt.Printf("empty query result\n")
			return nil, nil
		}

		fmt.Printf("result: %v\n", attributes.attrs)

		astTerm, err := ast.ParseTerm(attributes.attrs)

		if err != nil {
			fmt.Printf("Parse term error, source: %v, err: %v\n", attributes.attrs, err)
			return nil, nil
		}
		fmt.Printf("res term: %v\n", astTerm)

		err = pushToCache(context.FuncCache, cacheKey, astTerm, context.FuncCacheTTL)
		if err != nil {
			fmt.Printf("push to cache error: %v\n", err)
			return nil, nil
		}

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
