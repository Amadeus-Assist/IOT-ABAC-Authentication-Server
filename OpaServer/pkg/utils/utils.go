package utils

import (
	"math"
	"strings"
)

func Add(a int64, b int64) int64 {
	if a > math.MaxInt64-b {
		return math.MaxInt64
	}
	return a + b
}

func GetFirstTermAfterQuote(raw string, sign string) string {
	subStr := raw[strings.Index(raw, sign)+len(sign):]
	//fmt.Printf("subStr: %v\n", subStr)
	firstQuote := strings.Index(subStr, QUOTE)
	subStr2 := subStr[firstQuote+1:]
	secondQuote := strings.Index(subStr2, QUOTE)
	res := subStr2[:secondQuote]
	return res
}
