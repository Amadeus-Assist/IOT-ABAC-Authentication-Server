package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"path/filepath"
	"runtime"
)

type authResponse struct {
	Decision string `json:"decision"`
}

type authRequest struct {
	AccessRequest string `json:"access_request"`
}

func main() {
	authUrl := "http://localhost:8080/authz"

	arContentByte, err := ioutil.ReadFile(filepath.Join(GetProjectRoot(), "access_request/access_request_alice.txt"))
	if err != nil {
		fmt.Printf("error read file, err: %v\n", err)
		return
	}
	arContent := string(arContentByte)

	authReq := authRequest{AccessRequest: arContent}

	jsonBytes, err := json.Marshal(authReq)

	if err != nil {
		fmt.Printf("unable to marshal request: %v\n", string(jsonBytes))
		return
	}

	//reqst, err := http.NewRequest("POST", authUrl, bytes.NewBuffer(jsonBytes))
	//
	//if err != nil {
	//	fmt.Printf("error create new request, err: %v\n", err)
	//}
	//
	//reqst.Header.Set("Content-Type", "application/json")
	//
	//client

	resp, err := http.Post(authUrl, "application/json", bytes.NewBuffer(jsonBytes))
	if err != nil {
		fmt.Printf("error http post, err: %v\n", err)
		return
	}

	body, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		fmt.Printf("invalid response: %v\n", err)
		return
	}

	var decision authResponse
	err = json.Unmarshal(body, &decision)
	if err != nil {
		fmt.Printf("unmarshal error, response body: %v\n", string(body))
	}
	fmt.Printf("final decision %v\n", decision.Decision)
}

func GetProjectRoot() string {
	_, b, _, _ := runtime.Caller(0)
	currentPath := filepath.Dir(b)
	projectRootPath := filepath.Join(currentPath, "..")
	return projectRootPath
}
