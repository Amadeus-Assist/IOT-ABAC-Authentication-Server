package opa_server_config

import (
	"database/sql"
	"fmt"
	"github.com/open-policy-agent/opa/ast"
	"github.com/open-policy-agent/opa/rego"
	"github.com/open-policy-agent/opa/types"
)

type queriedAttributes struct {
	attrs string
}

func PrepareRego(context OPAServerContext) {
	rego.RegisterBuiltin2(&rego.Function{
		Name: "sql_query",
		Decl: types.NewFunction(
			types.Args(types.S, types.NewArray(make([]types.Type, 0), types.S)),
			types.A,
		),
	}, func(_ rego.BuiltinContext, tempTerm, paramsTerm *ast.Term) (*ast.Term, error) {

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

		attributes.attrs = `{
"name": "zhangsan"
}`

		fmt.Printf("result: %v\n", attributes.attrs)

		astTerm, err := ast.ParseTerm(attributes.attrs)

		if err != nil {
			fmt.Printf("Parse term error, source: %v, err: %v\n", attributes.attrs, err)
			return nil, nil
		}
		fmt.Printf("res term: %v\n", astTerm)

		return astTerm, nil
	})
}
