package utils

const (
	MYSQL = "mysql"
	TRUE = "true"
	FALSE = "false"
	OBJECT = "obj"
	ID = "id"
	LOCAL_BASIC_DS = "abac"
	HIERARCHY_QUERY_TEMP = "SELECT hierarchy FROM #{tableName} where obj_id=? LIMIT 1"
	TABLENAME = "#{tableName}"
	HIERARCHY_TABLE = "object_policy_hierarchy"
	POLICY_QUERY_TEMP = "SELECT content from #{tableName} where ref=? LIMIT 1"
	POLICY_REPOSITORY_TABLE = "rego_policy_repository"
	HIERARCHY_SEP = "/"
	EWMA_RATE = 0.125
	QUOTE = `"`
	SQL_QUERY_TERM_REGEX = `^.*sql_query *\( *".+" *, *".+" *, *\[.*\] *\).*$`
	API_GET_OBJ_TERM_REGEX = `^.*api_get_obj_term *\( *".+" *, *\[.*\] *\).*$`
)
