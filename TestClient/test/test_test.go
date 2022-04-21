package test

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"path/filepath"
	"runtime"
	"testing"
)

type DevRegRequest struct {
	DevId   string `json:"device_id"`
	DevType string `json:"device_type"`
}

type DevRegResponse struct {
	Token   string `json:"token"`
	Message string `json:"message"`
}

func TestDevRegister(t *testing.T) {
	devRegUrl := "http://localhost:8080/authz/register/dev"

	devId := "door_columbia_seas_702"
	devType := "office_door_columbia_seas"

	regReq := DevRegRequest{DevId: devId, DevType: devType}

	jsonBytes, err := json.Marshal(regReq)

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

	resp, err := http.Post(devRegUrl, "application/json", bytes.NewBuffer(jsonBytes))
	if err != nil {
		fmt.Printf("error http post, err: %v\n", err)
		return
	}

	body, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		fmt.Printf("invalid response: %v\n", err)
		return
	}

	fmt.Printf("resposne: %v\n", string(body))

	var response DevRegResponse
	err = json.Unmarshal(body, &response)
	if err != nil {
		fmt.Printf("unmarshal error, response body: %v\n", string(body))
	}
	if resp.StatusCode == 200 {
		fmt.Printf("token: %v\n", response.Token)
	} else {
		fmt.Printf("err msg: %v\n", response.Message)
	}
	//fmt.Printf("final decision %v\n", decision.Decision)
}

type QueryActionsResponse struct {
	Actions string `json:"actions"`
}

func TestQueryActions(t *testing.T) {
	devId := "door_columbia_seas_702"
	queryUrl := "http://localhost:8080/authz/query-actions/" + devId

	resp, err := http.Get(queryUrl)

	if err != nil {
		fmt.Printf("error http get, err: %v\n", err)
		return
	}

	body, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		fmt.Printf("invalid response: %v\n", err)
		return
	}

	var response QueryActionsResponse
	err = json.Unmarshal(body, &response)
	if err != nil {
		fmt.Printf("unmarshal error, response body: %v\n", string(body))
	}

	fmt.Printf("queried actions: %v\n", response.Actions)
}

type EvalRequest struct {
	SubUsername   string `json:"sub_username"`
	SubPassword   string `json:"sub_user_password"`
	ObjDevId      string `json:"obj_dev_id"`
	ObjToken      string `json:"obj_token"`
	AccessRequest string `json:"access_request"`
}

type EvalResponse struct {
	Decision string `json:"decision"`
	Message  string `json:"message"`
}

func TestEvalWithAuthentication(t *testing.T) {
	authUrl := "http://localhost:8080/authz/eval"

	objId := "door_columbia_seas_702"
	objToken := "mK3nTIxb67kKtcGmM1kMeLH_lZC3czW7"
	subUsername := "alice_5832"
	subPassword := "123456"

	arContentByte, err := ioutil.ReadFile(filepath.Join(GetProjectRoot(), "access_request/access_request_alice.txt"))
	if err != nil {
		fmt.Printf("error read file， err: %v\n", err)
		return
	}
	arContent := string(arContentByte)

	evalRequest := EvalRequest{SubUsername: subUsername, SubPassword: subPassword, ObjDevId: objId,
		ObjToken: objToken, AccessRequest: arContent}

	jsonBytes, err := json.Marshal(evalRequest)

	if err != nil {
		fmt.Printf("unable to marshal request: %v\n", string(jsonBytes))
		return
	}

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

	//fmt.Printf("resposne: %v\n", string(body))

	var response EvalResponse
	err = json.Unmarshal(body, &response)
	if err != nil {
		fmt.Printf("unmarshal error, response body: %v\n", string(body))
	}
	if resp.StatusCode == 200 {
		fmt.Printf("decision: %v\n", response.Decision)
	} else {
		fmt.Printf("err msg: %v\n", response.Message)
	}

}

func GetProjectRoot() string {
	_, b, _, _ := runtime.Caller(0)
	currentPath := filepath.Dir(b)
	projectRootPath := filepath.Join(currentPath, "..")
	return projectRootPath
}
