package test

import (
	"fmt"
	"github.com/bluele/gcache"
	"testing"
	"time"
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
