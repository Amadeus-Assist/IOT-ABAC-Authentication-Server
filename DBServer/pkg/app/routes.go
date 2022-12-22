package app

import "github.com/gin-gonic/gin"

func (s *Server) Routes() *gin.Engine {
	router := s.router

	// group all routes under /v1/api

	// v1 := router.Group("/test")
	// {
	// 	v1.GET("/:id", s.DBTest())
	// }

	v2 := router.Group("/find_db_access")
	{
		v2.GET("/:user_id/:table_name", s.FindDBAccess())
	}

	// router.POST("/test", s.DBTest())

	v5 := router.Group("/find_uesr_attrs")
	{
		v5.GET("/:id", s.FindUserAttrs())
	}

	v6 := router.Group("/find_policy")
	{
		v6.GET("/:ref", s.FindPolicy())
	}

	v7 := router.Group("/find_hierarchy")
	{
		v7.GET("/:obj_id/:action", s.FindHierarchy())
	}

	v8 := router.Group("/find_user_check_info")
	{
		v8.GET("/:user_id", s.FindUserCheckInfo())
	}

	v9 := router.Group("/find_dev_check_info")
	{
		v9.GET("/:dev_id", s.FindDevCheckInfo())
	}

	v10 := router.Group("/find_dev_actions")
	{
		v10.GET("/:dev_id", s.FindDevActions())
	}

	v11 := router.Group("/find_dev_attrs")
	{
		v11.GET("/:dev_id", s.FindDevAttrs())
	}

	router.POST("/insert_user_attrs", s.InsertPolicy())

	router.POST("/insert_perm_info", s.InsertPermInfo())

	router.POST("/update_db_allow", s.UpdateSecureDBAllow())

	router.POST("/update_db_deny", s.UpdateSecureDBDeny())

	router.POST("/jwt", s.SendJWT())

	return router
}
