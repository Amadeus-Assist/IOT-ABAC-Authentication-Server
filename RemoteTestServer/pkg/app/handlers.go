package app

import (
	"database/sql"
	"fmt"
	"github.com/gin-gonic/gin"
	"net/http"
	"time"
)

type userAttrs struct {
	attrs string
}

func (s *Server) Query() gin.HandlerFunc {
	return func(context *gin.Context) {
		context.Header("Content-Type", "application/json")

		id := context.Param("id")
		sqlTemp := "SELECT attrs FROM user_attrs WHERE user_id=?"
		res, err := s.conn.Query(sqlTemp, id)
		fmt.Printf("query temp: %v, params: %v\n", sqlTemp, id)

		if err != nil {
			fmt.Printf("Unable to execute sql_query, template: %v, params: %v, err: %v\n",
				sqlTemp, id, err)
			context.JSON(http.StatusBadRequest, nil)
			return
		}

		defer func(res *sql.Rows) {
			err := res.Close()
			if err != nil {
				fmt.Printf("close res err: %v\n", err)
			}
		}(res)

		var attributes userAttrs

		if res.Next() {
			if err := res.Scan(&attributes.attrs); err != nil {
				fmt.Printf("scan err: %v\n", err)
				context.JSON(http.StatusBadRequest, nil)
				return
			}
		} else {
			fmt.Printf("empty query result\n")
			context.JSON(http.StatusBadRequest, nil)
			return
		}

		fmt.Printf("attrs: %v\n", attributes.attrs)

		time.Sleep(1*time.Second)

		context.String(http.StatusOK, attributes.attrs)
	}
}