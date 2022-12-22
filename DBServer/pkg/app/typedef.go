package app

type UserAttrs struct {
	User_id string `json:"user_id"`
	Attrs   string `json:"attrs"`
}

type DevAttrs struct {
	Dev_id string
	Attrs  string
}

type DBAccess struct {
	User_id        string `json:"user_id"`
	Table_name     string `json:"table_name"`
	Db_access_date string `json:"db_access_date"`
	Db_deny_date   string `json:"db_deny_date"`
}

type JWTRequest struct {
	ClientMessage string `json:"client_message"`
}

type Policy struct {
	Ref     string `json:"ref"`
	Content string `json:"content"`
}

type Hierarchy struct {
	Obj_id    string `json:"obj_id"`
	Action    string `json:"action"`
	Hierarchy string `json:"hierarchy"`
}

type UserCheckInfo struct {
	User_id  string `json:"user_id"`
	Password string `json:"password"`
}

type DevCheckInfo struct {
	Dev_id   string `json:"dev_id"`
	Dev_type string `json:"dev_type"`
	Token    string `json:"token"`
	Attrs    string `json:"attrs"`
}

type DevActions struct {
	Dev_id  string `json:"dev_id"`
	Actions string `json:"actions"`
}

type InsertPolicyRequest struct {
	Ref     string `json:"ref"`
	Content string `json:"content"`
}

type UpdatePolicyRequest struct {
	Ref     string `json:"ref"`
	Content string `json:"content"`
}

type InsertPermInfoQueryRequest struct {
	User_id        string `json:"user_id"`
	Tbl_name       string `json:"tbl_name"`
	Db_access_date string `json:"db_access_date"`
	Db_deny_date   string `json:"db_deny_date"`
}

type UpdateSecureDBAllowRequest struct {
	User_id        string `json:"user_id"`
	Tbl_name       string `json:"tbl_name"`
	Db_access_date string `json:"db_access_date"`
}

type UpdateSecureDBDenyRequest struct {
	User_id      string `json:"user_id"`
	Tbl_name     string `json:"tbl_name"`
	Db_deny_date string `json:"db_deny_date"`
}

type InsertObjectHierarchyRequest struct {
	Obj_id    string `json:"obj_id"`
	Action    string `json:"action"`
	Hierarchy string `json:"hierarchy"`
}

type InsertUserAttrsRequest struct {
	User_id  string `json:"user_id"`
	Password string `json:"password"`
	Attrs    string `json:"attrs"`
}

type InsertDevInfoRequest struct {
	Dev_id   string `json:"dev_id"`
	Dev_type string `json:"dev_type"`
	Token    string `json:"token"`
	Attrs    string `json:"attrs"`
}

type InsertDevInfoFullRequest struct {
	Dev_id   string `json:"dev_id"`
	Dev_type string `json:"dev_type"`
	Action   string `json:"action"`
	Token    string `json:"token"`
	Attrs    string `json:"attrs"`
}

type UpdateObjectHierarchyRequest struct {
	Hierarchy string `json:"hierarchy"`
	Obj_id    string `json:"obj_id"`
	Action    string `json:"action"`
}

type UpdateUserAttrsRequest struct {
	User_id  string `json:"user_id"`
	Password string `json:"password"`
	Attrs    string `json:"attrs"`
}

const (
	FindPolicyQuery            = "SELECT ref, content FROM rego_policy_repository WHERE ref=? LIMIT 1"
	InsertPolicyQuery          = "INSERT INTO rego_policy_repository(ref, content) VALUES(?, ?)" //use generated keys?
	UpdatePolicyQuery          = "UPDATE rego_policy_repository SET content=? WHERE ref=?"
	FindHierarchyQuery         = "SELECT obj_id, action, hierarchy FROM object_action_policy_hierarchy WHERE obj_id=? AND action=? LIMIT 1"
	InsertObjectHierarchyQuery = "INSERT INTO object_action_policy_hierarchy(obj_id, action, hierarchy) VALUES(?, ?, ?)"
	UpdateObjectHierarchyQuery = "UPDATE object_action_policy_hierarchy SET hierarchy=? WHERE obj_id=? AND action=?"
	FindUserAttrsQuery         = "SELECT user_id, attrs FROM user_attrs WHERE user_id=? LIMIT 1"
	InsertUserAttrsQuery       = "INSERT INTO user_attrs(user_id, pwd, attrs) VALUES(?, ?, ?)"
	UpdateUserAttrsQuery       = "UPDATE user_attrs SET attrs=? WHERE user_id=?"
	FindUserCheckInfoQuery     = "SELECT user_id, pwd FROM user_attrs WHERE user_id=? LIMIT 1"
	FindDevCheckInfoQuery      = "SELECT dev_id, dev_type, token FROM dev_info WHERE dev_id=? LIMIT 1"
	InsertDevInfoQuery         = "INSERT INTO dev_info(dev_id, dev_type, token, attrs) VALUES(?, ?, ?, ?)"
	FindDevActionsQuery        = "SELECT dev_id, actions FROM dev_info WHERE dev_id=? LIMIT 1"
	FindDevAttrsQuery          = "SELECT dev_id, attrs FROM dev_info WHERE dev_id=? LIMIT 1"
	InsertDevInfoFullQuery     = "INSERT INTO dev_info(dev_id, dev_type, actions, token, attrs) VALUES(?, ?, ?, ?, ?)"
	InsertPermInfoQuery        = "INSERT INTO db_access(user_id, tbl_name, db_access_date, db_deny_date) VALUES(?, ?, ?, ?)"
	FindAccessDateQuery        = "SELECT user_id, tbl_name, db_access_date, db_deny_date FROM db_access WHERE user_id=? AND tbl_name=? LIMIT 1"
	UpdateSecureDBAllowQuery   = "UPDATE db_access SET db_access_date=? WHERE user_id=? AND tbl_name=?"
	UpdateSecureDBDenyQuery    = "UPDATE db_access SET db_deny_date=? WHERE user_id=? AND tbl_name=?"
)
