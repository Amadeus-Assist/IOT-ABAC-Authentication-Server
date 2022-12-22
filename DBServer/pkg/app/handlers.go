package app

import (
	sqlctx "context"
	"database/sql"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"log"
	"net/http"
	"regexp"
	"strconv"
	"strings"
	"time"

	"github.com/gin-gonic/gin"
	_ "github.com/go-sql-driver/mysql"
	jwt "github.com/golang-jwt/jwt/v4"
)

func (s *Server) FindUserAttrs() gin.HandlerFunc { //don't have enough permission
	return func(context *gin.Context) {
		context.Header("Content-Type", "application/json")

		id := context.Param("id")

		if !s.checkAuthServerPerm(id, "user_attrs") {
			context.String(http.StatusBadRequest, "Don't have access to DB")
			return
		}

		res, err := s.conn.Query(FindUserAttrsQuery, id)
		fmt.Printf("query temp: %v, params: %v\n", FindUserAttrsQuery, id)

		if err != nil {
			fmt.Printf("Unable to execute sql_query, template: %v, params: %v, err: %v\n",
				FindUserAttrsQuery, id, err)
			context.JSON(http.StatusBadRequest, nil)
			return
		}

		defer func(res *sql.Rows) {
			err := res.Close()
			if err != nil {
				fmt.Printf("close res err: %v\n", err)
			}
		}(res)

		var result UserAttrs

		if res.Next() {
			if err := res.Scan(&result.User_id, &result.Attrs); err != nil {
				fmt.Printf("scan err: %v\n", err)
				context.JSON(http.StatusBadRequest, nil)
				return
			}
		} else {
			fmt.Printf("empty query result\n")
			context.JSON(http.StatusBadRequest, nil)
			return
		}

		fmt.Printf("attrs: %+v\n", result.Attrs)

		time.Sleep(100 * time.Millisecond)

		ret, err := json.Marshal(result)
		if err != nil {
			fmt.Println(err)
			return
		}
		context.String(http.StatusOK, string(ret))
	}
}

func (s *Server) FindUserCheckInfo() gin.HandlerFunc {
	return func(context *gin.Context) {
		context.Header("Content-Type", "application/json")

		user_id := context.Param("user_id")
		res, err := s.conn.Query(FindUserCheckInfoQuery, user_id)
		fmt.Printf("query temp: %v, params: %v \n", FindUserCheckInfoQuery, user_id)

		if err != nil {
			fmt.Printf("Unable to execute sql_query, template: %v, params: %v, err: %v\n",
				FindUserCheckInfoQuery, user_id, err)
			context.JSON(http.StatusBadRequest, nil)
			return
		}

		defer func(res *sql.Rows) {
			err := res.Close()
			if err != nil {
				fmt.Printf("close res err: %v\n", err)
			}
		}(res)

		var result UserCheckInfo

		if res.Next() {
			if err := res.Scan(&result.User_id, &result.Password); err != nil {
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

func (s *Server) FindDBAccess() gin.HandlerFunc {
	fmt.Println("here find db access")
	return func(context *gin.Context) {
		context.Header("Content-Type", "application/json")

		user_id := context.Param("user_id")
		table_name := context.Param("table_name")

		res, err := s.conn.Query(FindAccessDateQuery, user_id, table_name)
		fmt.Printf("query temp: %v, params: %v, %v\n", FindAccessDateQuery, user_id, table_name)

		if err != nil {
			fmt.Printf("Unable to execute sql_query, template: %v, params: %v, %v, err: %v\n",
				FindAccessDateQuery, user_id, table_name, err)
			context.JSON(http.StatusBadRequest, nil)
			return
		}

		defer func(res *sql.Rows) {
			err := res.Close()
			if err != nil {
				fmt.Printf("close res err: %v\n", err)
			}
		}(res)

		var result DBAccess

		if res.Next() {
			if err := res.Scan(&result.User_id, &result.Table_name, &result.Db_access_date, &result.Db_deny_date); err != nil {
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

func (s *Server) InsertUserAttrs() gin.HandlerFunc {
	return func(context *gin.Context) {
		context.Header("Content-Type", "application/json")

		reqBody, err := ioutil.ReadAll(context.Request.Body)

		var reqdata InsertUserAttrsRequest
		json.Unmarshal(reqBody, &reqdata)
		ctx, cancelfunc := sqlctx.WithTimeout(sqlctx.Background(), 5*time.Second)
		defer cancelfunc()
		stmt, err := s.conn.PrepareContext(ctx, InsertUserAttrsQuery)
		if err != nil {
			fmt.Printf("Error %s when preparing SQL statement", err)
			return
		}

		defer stmt.Close()
		res, err := stmt.ExecContext(ctx, reqdata.User_id, reqdata.Password, reqdata.Attrs)
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

func (s *Server) InsertPermInfo() gin.HandlerFunc {
	return func(context *gin.Context) {
		context.Header("Content-Type", "application/json")
		reqBody, err := ioutil.ReadAll(context.Request.Body)
		// fmt.Printf(string(reqBody))
		var reqdata InsertPermInfoQueryRequest
		json.Unmarshal(reqBody, &reqdata)
		fmt.Printf("%+v\n", reqdata)
		ctx, cancelfunc := sqlctx.WithTimeout(sqlctx.Background(), 5*time.Second)
		defer cancelfunc()
		stmt, err := s.conn.PrepareContext(ctx, InsertPermInfoQuery)
		if err != nil {
			fmt.Printf("Error %s when preparing SQL statement", err)
			return
		}

		defer stmt.Close()
		res, err := stmt.ExecContext(ctx, reqdata.User_id, reqdata.Tbl_name, reqdata.Db_access_date, reqdata.Db_deny_date)
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
		return
	}
}

func (s *Server) UpdateSecureDBAllow() gin.HandlerFunc {
	return func(context *gin.Context) {
		context.Header("Content-Type", "application/json")

		reqBody, err := ioutil.ReadAll(context.Request.Body)
		fmt.Printf(string(reqBody))
		var reqdata UpdateSecureDBAllowRequest

		json.Unmarshal(reqBody, &reqdata)
		ctx, cancelfunc := sqlctx.WithTimeout(sqlctx.Background(), 5*time.Second)
		defer cancelfunc()
		stmt, err := s.conn.PrepareContext(ctx, UpdateSecureDBAllowQuery)
		if err != nil {
			fmt.Printf("Error %s when preparing SQL statement", err)
			return
		}

		defer stmt.Close()
		res, err := stmt.ExecContext(ctx, reqdata.Db_access_date, reqdata.User_id, reqdata.Tbl_name)
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

func (s *Server) UpdateSecureDBDeny() gin.HandlerFunc {
	return func(context *gin.Context) {
		context.Header("Content-Type", "application/json")

		reqBody, err := ioutil.ReadAll(context.Request.Body)
		fmt.Printf(string(reqBody))
		var reqdata UpdateSecureDBDenyRequest

		json.Unmarshal(reqBody, &reqdata)
		ctx, cancelfunc := sqlctx.WithTimeout(sqlctx.Background(), 5*time.Second)
		defer cancelfunc()
		stmt, err := s.conn.PrepareContext(ctx, UpdateSecureDBDenyQuery)
		if err != nil {
			fmt.Printf("Error %s when preparing SQL statement", err)
			return
		}

		defer stmt.Close()
		res, err := stmt.ExecContext(ctx, reqdata.Db_deny_date, reqdata.User_id, reqdata.Tbl_name)
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

func (s *Server) UpdateUserAttrs() gin.HandlerFunc {
	return func(context *gin.Context) {
		context.Header("Content-Type", "application/json")

		reqBody, err := ioutil.ReadAll(context.Request.Body)
		fmt.Printf(string(reqBody))
		var reqdata UpdateUserAttrsRequest

		json.Unmarshal(reqBody, &reqdata)
		ctx, cancelfunc := sqlctx.WithTimeout(sqlctx.Background(), 5*time.Second)
		defer cancelfunc()
		stmt, err := s.conn.PrepareContext(ctx, UpdateSecureDBAllowQuery)
		if err != nil {
			fmt.Printf("Error %s when preparing SQL statement", err)
			return
		}

		defer stmt.Close()
		res, err := stmt.ExecContext(ctx, reqdata.User_id, reqdata.Password, reqdata.Attrs)
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

func (s *Server) SendJWT() gin.HandlerFunc {
	return func(context *gin.Context) {
		context.Header("Content-Type", "application/json")

		reqBody, err := ioutil.ReadAll(context.Request.Body)
		fmt.Printf(string(reqBody))

		if err != nil {
			fmt.Printf("server: could not read request body: %s\n", err)
		}

		var reqdata JWTRequest
		json.Unmarshal(reqBody, &reqdata)
		tokenString := reqdata.ClientMessage

		testkey := "12345"
		parts := strings.Split(tokenString, ".")
		method := jwt.GetSigningMethod("HS256")
		err2 := method.Verify(strings.Join(parts[0:2], "."), parts[2], []byte(testkey))
		if err2 != nil {
			fmt.Printf("Error while verifying key: %v", err2)
		} else {
			fmt.Println("Correct key")
		}

		token, err := jwt.Parse(tokenString, func(token *jwt.Token) (interface{}, error) {
			// Don't forget to validate the alg is what you expect:
			if _, ok := token.Method.(*jwt.SigningMethodHMAC); !ok {
				return nil, fmt.Errorf("Unexpected signing method: %v", token.Header["alg"])
			}
			return []byte(testkey), nil
		})

		if claims, ok := token.Claims.(jwt.MapClaims); ok && token.Valid {
			fmt.Printf("%+v \n", claims)
			// fmt.Println(claims["sub"])
			s.accessDateUpdate(claims["user"].(string), claims["sub"].(string))
			context.String(http.StatusOK, `{"server_message": "JWT received!"}`)
		} else {
			fmt.Println(err)
			context.String(http.StatusBadRequest, `{"server_message": "wrong JWT signature!"}`)
		}
	}

}

func (s *Server) accessDateUpdate(user_id string, dbauth string) { //given db_auth, do the proper update
	//parse dbauth
	list := strings.Split(dbauth, ",")
	mp := make(map[string]string)
	for _, line := range list {
		kv := strings.Split(line, ":")
		mp[kv[0]] = kv[1]
	}
	fmt.Println("%+v", mp)
	//query date
	for tbl, element := range mp {
		res, err := s.conn.Query(FindAccessDateQuery, user_id, tbl)
		fmt.Printf("query temp: %v, params: %v, %v\n", FindAccessDateQuery, user_id, tbl)

		if err != nil {
			fmt.Printf("Unable to execute sql_query, template: %v, params: %v, %v, err: %v\n",
				FindAccessDateQuery, user_id, tbl, err)
			return
		}

		defer func(res *sql.Rows) {
			err := res.Close()
			if err != nil {
				fmt.Printf("close res err: %v\n", err)
			}
		}(res)

		var result DBAccess

		if res.Next() {
			if err := res.Scan(&result.User_id, &result.Table_name, &result.Db_access_date, &result.Db_deny_date); err != nil {
				fmt.Printf("scan err: %v\n", err)
				return
			}
		} else {
			fmt.Printf("empty query result\n")
			return
		}

		fmt.Printf("db perm: %+v\n", result)

		//update date
		days := 0
		if strings.Contains(element, "always") {
			days = 9999
		} else if strings.Contains(element, "once") {
			if strings.Contains(element, "allow") {
				var mk = Mapkey{user_id, tbl}
				s.allow_once[mk] = true
			}
			days = 0
		} else {
			re := regexp.MustCompile("[0-9]+")
			days, err = strconv.Atoi(re.FindAllString(element, -1)[0])
			if err != nil {
				days = 0
			}
		}
		// fmt.Println(days)
		if strings.Contains(element, "allow") {
			newallowdate, _ := time.Parse("2006-01-02", result.Db_access_date)
			newallowdate = time.Now().AddDate(0, 0, days)
			fmt.Println("new allow date : %v", newallowdate.Format("2006-01-02"))
			ctx, cancelfunc := sqlctx.WithTimeout(sqlctx.Background(), 5*time.Second)
			defer cancelfunc()
			stmt, err := s.conn.PrepareContext(ctx, UpdateSecureDBAllowQuery)
			if err != nil {
				fmt.Printf("Error %s when preparing SQL statement", err)
				return
			}
			defer stmt.Close()

			res, err := stmt.ExecContext(ctx, newallowdate.Format("2006-01-02"), &result.User_id, &result.Table_name)
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
		} else {
			newdenydate, _ := time.Parse("2006-01-02", result.Db_deny_date)
			newdenydate = time.Now().AddDate(0, 0, days)
			fmt.Println("new deny date : %v", newdenydate.Format("2006-01-02"))
			ctx, cancelfunc := sqlctx.WithTimeout(sqlctx.Background(), 5*time.Second)
			defer cancelfunc()
			stmt, err := s.conn.PrepareContext(ctx, UpdateSecureDBDenyQuery)
			if err != nil {
				fmt.Printf("Error %s when preparing SQL statement", err)
				return
			}
			defer stmt.Close()

			res, err := stmt.ExecContext(ctx, newdenydate.Format("2006-01-02"), &result.User_id, &result.Table_name)
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
		}

	}
}

func (s *Server) checkAuthServerPerm(user_id string, tbl_name string) bool {
	var mk = Mapkey{user_id, tbl_name}
	if s.allow_once[mk] == true {
		delete(s.allow_once, mk)
		return true
	}
	res, err := s.conn.Query(FindAccessDateQuery, user_id, tbl_name)
	fmt.Printf("query temp: %v, params: %v, %v\n", FindAccessDateQuery, user_id, tbl_name)

	if err != nil {
		fmt.Printf("Unable to execute sql_query, template: %v, params: %v, %v, err: %v\n",
			FindAccessDateQuery, user_id, tbl_name, err)
		return false
	}

	defer func(res *sql.Rows) {
		err := res.Close()
		if err != nil {
			fmt.Printf("close res err: %v\n", err)
		}
	}(res)

	var result DBAccess

	if res.Next() {
		if err := res.Scan(&result.User_id, &result.Table_name, &result.Db_access_date, &result.Db_deny_date); err != nil {
			fmt.Printf("scan err: %v\n", err)
			return false
		}
	} else {
		fmt.Printf("empty query result\n")
		return false
	}

	fmt.Printf("actions: %+v\n", result)
	allowdate, _ := time.Parse("2006-01-02", result.Db_access_date)
	if time.Now().Before(allowdate) {
		return true
	}
	return false
}
