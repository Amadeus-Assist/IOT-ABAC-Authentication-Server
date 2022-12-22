package app

import (
	sqlctx "context"
	"database/sql"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"log"
	"net/http"
	"time"

	"github.com/gin-gonic/gin"
	_ "github.com/go-sql-driver/mysql"
)

func (s *Server) FindPolicy() gin.HandlerFunc {
	return func(context *gin.Context) {
		context.Header("Content-Type", "application/json")

		ref := context.Param("ref")
		res, err := s.conn.Query(FindPolicyQuery, ref)
		fmt.Printf("query temp: %v, params: %v\n", FindPolicyQuery, ref)

		if err != nil {
			fmt.Printf("Unable to execute sql_query, template: %v, params: %v, err: %v\n",
				FindPolicyQuery, ref, err)
			context.JSON(http.StatusBadRequest, nil)
			return
		}

		defer func(res *sql.Rows) {
			err := res.Close()
			if err != nil {
				fmt.Printf("close res err: %v\n", err)
			}
		}(res)

		var result Policy

		if res.Next() {
			if err := res.Scan(&result.Ref, &result.Content); err != nil {
				fmt.Printf("scan err: %v\n", err)
				context.JSON(http.StatusBadRequest, nil)
				return
			}
		} else {
			fmt.Printf("empty query result\n")
			context.JSON(http.StatusBadRequest, nil)
			return
		}

		fmt.Printf("policy: %+v\n", result)

		time.Sleep(100 * time.Millisecond)

		ret, err := json.Marshal(result)
		if err != nil {
			fmt.Println(err)
			return
		}
		context.String(http.StatusOK, string(ret))
	}
}

func (s *Server) FindHierarchy() gin.HandlerFunc {
	return func(context *gin.Context) {
		context.Header("Content-Type", "application/json")

		obj_id := context.Param("obj_id")
		action := context.Param("action")
		res, err := s.conn.Query(FindHierarchyQuery, obj_id, action)
		fmt.Printf("query temp: %v, params: %v, %v\n", FindHierarchyQuery, obj_id, action)

		if err != nil {
			fmt.Printf("Unable to execute sql_query, template: %v, params: %v, %v, err: %v\n",
				FindHierarchyQuery, obj_id, action, err)
			context.JSON(http.StatusBadRequest, nil)
			return
		}

		defer func(res *sql.Rows) {
			err := res.Close()
			if err != nil {
				fmt.Printf("close res err: %v\n", err)
			}
		}(res)

		var result Hierarchy

		if res.Next() {
			if err := res.Scan(&result.Obj_id, &result.Action, &result.Hierarchy); err != nil {
				fmt.Printf("scan err: %v\n", err)
				context.JSON(http.StatusBadRequest, nil)
				return
			}
		} else {
			fmt.Printf("empty query result\n")
			context.JSON(http.StatusBadRequest, nil)
			return
		}

		fmt.Printf("hierarchy: %+v\n", result)

		time.Sleep(100 * time.Millisecond)

		ret, err := json.Marshal(result)
		if err != nil {
			fmt.Println(err)
			return
		}
		context.String(http.StatusOK, string(ret))
	}
}

func (s *Server) FindDevCheckInfo() gin.HandlerFunc {
	return func(context *gin.Context) {
		context.Header("Content-Type", "application/json")

		dev_id := context.Param("dev_id")
		res, err := s.conn.Query(FindDevCheckInfoQuery, dev_id)
		fmt.Printf("query temp: %v, params: %v \n", FindDevCheckInfoQuery, dev_id)

		if err != nil {
			fmt.Printf("Unable to execute sql_query, template: %v, params: %v, err: %v\n",
				FindDevCheckInfoQuery, dev_id, err)
			context.JSON(http.StatusBadRequest, nil)
			return
		}

		defer func(res *sql.Rows) {
			err := res.Close()
			if err != nil {
				fmt.Printf("close res err: %v\n", err)
			}
		}(res)

		var result DevCheckInfo

		if res.Next() {
			if err := res.Scan(&result.Dev_id, &result.Dev_type, &result.Token); err != nil {
				fmt.Printf("scan err: %v\n", err)
				context.JSON(http.StatusBadRequest, nil)
				return
			}
		} else {
			fmt.Printf("empty query result\n")
			context.JSON(http.StatusBadRequest, nil)
			return
		}

		fmt.Printf("hierarchy: %+v\n", result)

		time.Sleep(100 * time.Millisecond)

		ret, err := json.Marshal(result)
		if err != nil {
			fmt.Println(err)
			return
		}
		context.String(http.StatusOK, string(ret))
	}
}

func (s *Server) FindDevAttrs() gin.HandlerFunc {
	return func(context *gin.Context) {
		context.Header("Content-Type", "application/json")

		dev_id := context.Param("dev_id")

		res, err := s.conn.Query(FindDevAttrsQuery, dev_id)
		fmt.Printf("query temp: %v, params: %v\n", FindUserAttrsQuery, dev_id)

		if err != nil {
			fmt.Printf("Unable to execute sql_query, template: %v, params: %v, err: %v\n",
				FindDevAttrsQuery, dev_id, err)
			context.JSON(http.StatusBadRequest, nil)
			return
		}

		defer func(res *sql.Rows) {
			err := res.Close()
			if err != nil {
				fmt.Printf("close res err: %v\n", err)
			}
		}(res)

		var result DevAttrs

		if res.Next() {
			if err := res.Scan(&result.Dev_id, &result.Attrs); err != nil {
				fmt.Printf("scan err: %v\n", err)
				context.JSON(http.StatusBadRequest, nil)
				return
			}
		} else {
			fmt.Printf("empty query result\n")
			context.JSON(http.StatusBadRequest, nil)
			return
		}

		fmt.Printf("attrs: %+v\n", result)

		time.Sleep(100 * time.Millisecond)

		ret, err := json.Marshal(result)
		if err != nil {
			fmt.Println(err)
			return
		}
		context.String(http.StatusOK, string(ret))
	}
}

func (s *Server) FindDevActions() gin.HandlerFunc {
	return func(context *gin.Context) {
		context.Header("Content-Type", "application/json")

		dev_id := context.Param("dev_id")

		res, err := s.conn.Query(FindDevActionsQuery, dev_id)
		fmt.Printf("query temp: %v, params: %v\n", FindDevActionsQuery, dev_id)

		if err != nil {
			fmt.Printf("Unable to execute sql_query, template: %v, params: %v, err: %v\n",
				FindDevActionsQuery, dev_id, err)
			context.JSON(http.StatusBadRequest, nil)
			return
		}

		defer func(res *sql.Rows) {
			err := res.Close()
			if err != nil {
				fmt.Printf("close res err: %v\n", err)
			}
		}(res)

		var result DevActions

		if res.Next() {
			if err := res.Scan(&result.Dev_id, &result.Actions); err != nil {
				fmt.Printf("scan err: %v\n", err)
				context.JSON(http.StatusBadRequest, nil)
				return
			}
		} else {
			fmt.Printf("empty query result\n")
			context.JSON(http.StatusBadRequest, nil)
			return
		}

		fmt.Printf("actions: %+v\n", result)

		time.Sleep(100 * time.Millisecond)

		ret, err := json.Marshal(result)
		if err != nil {
			fmt.Println(err)
			return
		}
		context.String(http.StatusOK, string(ret))
	}
}

func (s *Server) InsertPolicy() gin.HandlerFunc {
	return func(context *gin.Context) {
		context.Header("Content-Type", "application/json")

		ref := context.Param("ref")
		reqBody, err := ioutil.ReadAll(context.Request.Body)

		var reqdata InsertPolicyRequest
		json.Unmarshal(reqBody, &reqdata)
		ctx, cancelfunc := sqlctx.WithTimeout(sqlctx.Background(), 5*time.Second)
		defer cancelfunc()
		stmt, err := s.conn.PrepareContext(ctx, InsertPolicyQuery)
		if err != nil {
			fmt.Printf("Error %s when preparing SQL statement", err)
			return
		}

		defer stmt.Close()
		res, err := stmt.ExecContext(ctx, ref, reqdata.Content)
		if err != nil {
			fmt.Printf("Error %s when inserting row into products table", err)
			return
		}
		rows, err := res.RowsAffected()
		if err != nil {
			fmt.Printf("Error %s when finding rows affected", err)
			return
		}

		log.Printf("%d rows inserted ", rows)

		context.String(http.StatusOK, string(rows)+" rows inserted ")
	}
}

func (s *Server) InsertObjectHierarchy() gin.HandlerFunc {
	return func(context *gin.Context) {
		context.Header("Content-Type", "application/json")

		reqBody, err := ioutil.ReadAll(context.Request.Body)

		var reqdata InsertObjectHierarchyRequest
		json.Unmarshal(reqBody, &reqdata)
		ctx, cancelfunc := sqlctx.WithTimeout(sqlctx.Background(), 5*time.Second)
		defer cancelfunc()
		stmt, err := s.conn.PrepareContext(ctx, InsertObjectHierarchyQuery)
		if err != nil {
			fmt.Printf("Error %s when preparing SQL statement", err)
			return
		}

		defer stmt.Close()
		res, err := stmt.ExecContext(ctx, reqdata.Obj_id, reqdata.Action, reqdata.Hierarchy)
		if err != nil {
			fmt.Printf("Error %s when inserting row into products table", err)
			return
		}
		rows, err := res.RowsAffected()
		if err != nil {
			fmt.Printf("Error %s when finding rows affected", err)
			return
		}

		log.Printf("%d rows inserted ", rows)

		context.String(http.StatusOK, string(rows)+" rows inserted ")
	}
}

func (s *Server) InsertDevInfo() gin.HandlerFunc {
	return func(context *gin.Context) {
		context.Header("Content-Type", "application/json")

		reqBody, err := ioutil.ReadAll(context.Request.Body)

		var reqdata InsertDevInfoRequest
		json.Unmarshal(reqBody, &reqdata)
		ctx, cancelfunc := sqlctx.WithTimeout(sqlctx.Background(), 5*time.Second)
		defer cancelfunc()
		stmt, err := s.conn.PrepareContext(ctx, InsertDevInfoQuery)
		if err != nil {
			fmt.Printf("Error %s when preparing SQL statement", err)
			return
		}

		defer stmt.Close()
		res, err := stmt.ExecContext(ctx, reqdata.Dev_id, reqdata.Dev_type, reqdata.Token, reqdata.Attrs)
		if err != nil {
			fmt.Printf("Error %s when inserting row into products table", err)
			return
		}
		rows, err := res.RowsAffected()
		if err != nil {
			fmt.Printf("Error %s when finding rows affected", err)
			return
		}

		log.Printf("%d rows inserted ", rows)

		context.String(http.StatusOK, string(rows)+" rows inserted ")
	}
}

func (s *Server) InsertDevInfoFull() gin.HandlerFunc {
	return func(context *gin.Context) {
		context.Header("Content-Type", "application/json")

		reqBody, err := ioutil.ReadAll(context.Request.Body)

		var reqdata InsertDevInfoFullRequest
		json.Unmarshal(reqBody, &reqdata)
		ctx, cancelfunc := sqlctx.WithTimeout(sqlctx.Background(), 5*time.Second)
		defer cancelfunc()
		stmt, err := s.conn.PrepareContext(ctx, InsertDevInfoFullQuery)
		if err != nil {
			fmt.Printf("Error %s when preparing SQL statement", err)
			return
		}

		defer stmt.Close()
		res, err := stmt.ExecContext(ctx, reqdata.Dev_id, reqdata.Dev_type, reqdata.Action, reqdata.Token, reqdata.Attrs)
		if err != nil {
			fmt.Printf("Error %s when inserting row into products table", err)
			return
		}
		rows, err := res.RowsAffected()
		if err != nil {
			fmt.Printf("Error %s when finding rows affected", err)
			return
		}

		log.Printf("%d rows inserted ", rows)

		context.String(http.StatusOK, string(rows)+" rows inserted ")
	}
}

func (s *Server) UpdatePolicy() gin.HandlerFunc {
	return func(context *gin.Context) {
		context.Header("Content-Type", "application/json")

		ref := context.Param("ref")
		reqBody, err := ioutil.ReadAll(context.Request.Body)

		var reqdata UpdatePolicyRequest
		json.Unmarshal(reqBody, &reqdata)
		ctx, cancelfunc := sqlctx.WithTimeout(sqlctx.Background(), 5*time.Second)
		defer cancelfunc()
		stmt, err := s.conn.PrepareContext(ctx, InsertPolicyQuery)
		if err != nil {
			fmt.Printf("Error %s when preparing SQL statement", err)
			return
		}

		defer stmt.Close()
		res, err := stmt.ExecContext(ctx, ref, reqdata.Content)
		if err != nil {
			fmt.Printf("Error %s when inserting row into products table", err)
			return
		}
		rows, err := res.RowsAffected()
		if err != nil {
			fmt.Printf("Error %s when finding rows affected", err)
			return
		}

		log.Printf("%d rows inserted ", rows)

		context.String(http.StatusOK, string(rows)+" rows inserted ")
	}
}

func (s *Server) UpdateObjectHierarchy() gin.HandlerFunc {
	return func(context *gin.Context) {
		context.Header("Content-Type", "application/json")

		reqBody, err := ioutil.ReadAll(context.Request.Body)
		fmt.Printf(string(reqBody))
		var reqdata UpdateObjectHierarchyRequest

		json.Unmarshal(reqBody, &reqdata)
		ctx, cancelfunc := sqlctx.WithTimeout(sqlctx.Background(), 5*time.Second)
		defer cancelfunc()
		stmt, err := s.conn.PrepareContext(ctx, UpdateObjectHierarchyQuery)
		if err != nil {
			fmt.Printf("Error %s when preparing SQL statement", err)
			return
		}

		defer stmt.Close()
		res, err := stmt.ExecContext(ctx, reqdata.Hierarchy, reqdata.Obj_id, reqdata.Action)
		if err != nil {
			fmt.Printf("Error %s when inserting row into products table", err)
			return
		}
		rows, err := res.RowsAffected()
		if err != nil {
			fmt.Printf("Error %s when finding rows affected", err)
			return
		}

		log.Printf("%d rows inserted ", rows)

		context.String(http.StatusOK, string(rows)+" rows updated ")
		return
	}
}
