package opa_server_config

import "database/sql"

func PrepareServerSetting(db *sql.DB) error {
	context := OPAServerContext{SqlDB: db}

	PrepareRego(context)

	return nil
}
