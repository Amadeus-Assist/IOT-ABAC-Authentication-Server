package main

import (
	"context"
	"database/sql"
	"fmt"
	"strings"

	//"github.com/gin-contrib/cors"
	//"github.com/gin-gonic/gin"
	"encoding/json"
	_ "github.com/go-sql-driver/mysql"
	"github.com/open-policy-agent/opa/rego"
	"log"
	"os"
	//"OpaServer/pkg/api"
	//"OpaServer/pkg/app"
	//"OpaServer/pkg/repository"
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
	// I'm used to working with postgres, but feel free to use any db you like. You just have to change the driver
	// I'm not going to cover how to create a database here but create a database
	// and call it something along the lines of "weight tracker"
	connectionString := "root:123456@tcp(localhost:3306)/abac"

	// setup database connection
	db, err := setupDatabase(connectionString)

	if err != nil {
		return err
	}

	name := "zhangsan"

	res, err := db.Query("SELECT ref FROM columbia_members WHERE name = ?", name)
	if err != nil {
		log.Fatal(err)
	}

	defer func(db *sql.DB) {
		err := db.Close()
		if err != nil {

		}
	}(db)

	defer func(res *sql.Rows) {
		err := res.Close()
		if err != nil {

		}
	}(res)

	if res.Next() {

		var memRef memberRef
		err := res.Scan(&memRef.ref)

		if err != nil {
			log.Fatal(err)
		}

		fmt.Printf("%v\n", memRef)
	}

	ctx := context.Background()

	type1 := `[
	{
		"key":"1",
		"content":[
			"input.method==\"GET\"",
			"input.path==[\"salary\",input.subject.user]"
		]
	},{
		"key":"1",
		"content":[
			"input.ref==\"bob213\""
		]
	},{
		"key":"2",
		"content":[
			"input.role==\"super\""
		]
	}
]`

	var ruleSet []interface{}
	err = json.Unmarshal([]byte(type1), &ruleSet)
	if err != nil {
		fmt.Printf("Parse Json Error\n")
		return err
	}

	fmt.Printf("%v\n", ruleSet)

	ruleMap := make(map[string][]string)

	for i := range ruleSet {
		ruleInterface := ruleSet[i]
		rule := ruleInterface.(map[string]interface{})
		key, prsK := rule["key"]
		content, prsC := rule["content"]
		if prsK && prsC {
			ruleKey := "rule_" + key.(string)
			localRuleArr, prsRA := ruleMap[ruleKey]
			if !prsRA {
				localRuleArr = make([]string, 0)
				ruleMap[ruleKey] = localRuleArr
			}
			contentArr := content.([]interface{})
			var sb strings.Builder
			for j := range contentArr {
				sb.WriteString(contentArr[j].(string) + "\n")
			}
			ruleMap[ruleKey] = append(localRuleArr, sb.String())
		}
	}

	var sb strings.Builder

	sb.WriteString(`
package example.authz

default allow = false

PERMIT{
`)

	for ruleKey, _ := range ruleMap {
		sb.WriteString(ruleKey + "\n")
	}

	sb.WriteString(`}

`)
	for ruleKey, contentArr := range ruleMap {
		for i := range contentArr {
			sb.WriteString(fmt.Sprintf("%v {\n%v}\n\n", ruleKey, contentArr[i]))
		}
	}

	module := sb.String()
	fmt.Printf(module)
	//
	//	module := `
	//package example.authz
	//
	//default allow = false
	//
	//allow {
	//   input.method == "GET"
	//   input.path == ["salary", input.subject.user]
	//}
	//
	//allow {
	//    is_admin
	//}
	//
	//is_admin {
	//    input.subject.groups[_] = "admin"
	//}
	//`
	//
	input := map[string]interface{}{
		"method": "GET",
		"path":   []interface{}{"salary", "bob"},
		"subject": map[string]interface{}{
			"user":   "bob",
			"groups": []interface{}{"sales", "marketing"},
		},
		"ref":  "bob21",
		"role": "super",
	}

	rego := rego.New(
		rego.Query("data.example.authz.PERMIT"),
		rego.Module("example.rego", module),
		rego.Input(input),
	)

	// Run evaluation.
	rs, err := rego.Eval(ctx)
	if err != nil {
		panic(err)
	}

	// Inspect result.
	fmt.Println("allowed:", rs.Allowed())

	//// create storage dependency
	//storage := repository.NewStorage(db)
	//
	//// create router dependecy
	//router := gin.Default()
	//router.Use(cors.Default())
	//
	//// create user service
	//userService := api.NewUserService(storage)
	//
	//// create weight service
	//weightService := api.NewWeightService(storage)
	//
	//server := app.NewServer(router, userService, weightService)
	//
	//// start the server
	//err = server.Run()
	//
	//if err != nil {
	//	return err
	//}

	return nil
}

func setupDatabase(connString string) (*sql.DB, error) {
	// change "postgres" for whatever supported database you want to use
	db, err := sql.Open("mysql", connString)

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
