package api

import (
	"context"
	"encoding/json"
	"errors"
	"github.com/open-policy-agent/opa/rego"
	"log"
)

type OpaEvalService interface {
	Eval(request *OpaEvalRequest) (string, error)
}

type opaEvalService struct {
}

func NewOpaEvalService() OpaEvalService {
	return &opaEvalService{}
}

func (opa *opaEvalService) Eval(request *OpaEvalRequest) (string, error) {
	if request.AccessRequest == "" {
		return "", errors.New("empty access request in request")
	}

	if request.Policy == "" {
		return "", errors.New("empty policy in request")
	}

	var inputMap map[string]interface{}

	err := json.Unmarshal([]byte(request.AccessRequest), &inputMap)

	if err != nil {
		return "", errors.New("invalid access request format")
	}

	regoInst := rego.New(
		rego.Query("data.authz.policy.PERMIT"),
		rego.Module("authz.rego", request.Policy),
		rego.Input(inputMap))

	rs, err := regoInst.Eval(context.Background())

	if err != nil {
		log.Printf("rego eval error, %v\n", err)
		return "", err
	}
	if len(rs) == 0 {
		return FALSE, nil
	}
	if rs.Allowed() {
		return TRUE, nil
	}
	return FALSE, nil
}
