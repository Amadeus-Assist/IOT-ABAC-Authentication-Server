package app

import (
	"encoding/json"
	"fmt"
	"github.com/gin-gonic/gin"
	"math/rand"
	"net/http"
	"strconv"
	"time"
)

func (s *Server) ReorderTest() gin.HandlerFunc {
	return func(context *gin.Context) {
		context.Header("Content-Type", "application/json")
		count, err := strconv.Atoi(context.Param("count"))
		if err != nil {
			fmt.Printf("count cannot convert to integer\n")
			context.JSON(http.StatusBadRequest, nil)
			return
		}

		rd := rand.Intn(2)

		respContentMap := make(map[string]bool)

		respContentMap["allowed"] = rd == 0

		respContent, err := json.Marshal(respContentMap)

		if err != nil {
			fmt.Printf("Unable to generate response content\n")
			context.JSON(http.StatusBadRequest, nil)
			return
		}

		if count%200 < 100 {
			time.Sleep(100 * time.Millisecond)
			count++
		}

		context.String(http.StatusOK, string(respContent))
	}
}
