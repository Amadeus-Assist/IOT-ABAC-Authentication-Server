package api

const (
	TRUE  = "true"
	FALSE = "false"
)

type OpaEvalRequest struct {
	AccessRequest string `json:"access_request"`
}
