package api

import (
	"context"
	"github.com/open-policy-agent/opa/rego"
	"log"
)

type OpaEvalService interface {
	Eval(input map[string]interface{}, policy string) (string, error)
}

type opaEvalService struct {
}

func NewOpaEvalService() OpaEvalService {
	return &opaEvalService{}
}

func (opa *opaEvalService) Eval(input map[string]interface{}, policy string) (string, error) {
	// setup input and policy for rego instance then evaluate
	regoInst := rego.New(
		rego.Query("data.authz.policy.PERMIT"),
		rego.Module("authz.rego", policy),
		rego.Input(input))

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
