package opa_server_config

import "database/sql"

type OPAServerContext struct {
	SqlDB *sql.DB
}
