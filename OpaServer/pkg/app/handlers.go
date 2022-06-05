package app

import (
	"OpaServer/pkg/api"
	"OpaServer/pkg/utils"
	"database/sql"
	"encoding/json"
	"errors"
	"fmt"
	"github.com/gin-gonic/gin"
	"log"
	"net/http"
	"regexp"
	"sort"
	"strings"
	"time"
)

type queriedHierarchy struct {
	hierarchy string
}

type queriedPolicy struct {
	content string
}

type jsonRule struct {
	Key     string   `json:"key"`
	Content []string `json:"content"`
}

type ruleWithCost struct {
	content []string
	cost    int64
}

// Eval handle evaluation requests
func (s *Server) Eval() gin.HandlerFunc {
	return func(context *gin.Context) {
		context.Header("Content-Type", "application/json")

		var newEvalRequest api.OpaEvalRequest

		err := context.ShouldBindJSON(&newEvalRequest)

		fmt.Printf("request:\naccess_request: %v\n", newEvalRequest.AccessRequest)

		if err != nil {
			log.Printf("handler error: %v\n", err)
			response := map[string]string{
				"message": err.Error(),
			}
			context.JSON(http.StatusBadRequest, response)
			return
		}

		// retrieve input map and assemble policy associated to the request
		policy, input, err := s.preprocessEvalRequest(&newEvalRequest)
		if err != nil {
			log.Printf("handler error: %v\n", err)
			response := map[string]string{
				"message": err.Error(),
			}
			context.JSON(http.StatusBadRequest, response)
			return
		}

		// call service function for evaluation
		decision, err := s.opaEvalService.Eval(input, policy)

		if err != nil {
			log.Printf("eval request error: %v\n", err)
			response := map[string]string{
				"message": err.Error(),
			}
			context.JSON(http.StatusBadRequest, response)
			return
		}

		response := map[string]string{
			"decision": decision,
		}

		context.JSON(http.StatusOK, response)
	}
}

func (s *Server) preprocessEvalRequest(evalRequest *api.OpaEvalRequest) (string, map[string]interface{}, error) {
	if evalRequest.AccessRequest == "" {
		fmt.Printf("empty access request\n")
		return "", nil, errors.New("empty access_request in request")
	}

	var inputMap map[string]interface{}

	// parse the input json to map
	err := json.Unmarshal([]byte(evalRequest.AccessRequest), &inputMap)

	if err != nil {
		return "", nil, errors.New("invalid access request format")
	}

	// check sub, obj, action fields all exist
	objInterface, prs := inputMap[utils.OBJECT]
	if !prs {
		return "", nil, errors.New("invalid access request format")
	}
	objMap := objInterface.(map[string]interface{})
	objIdInterface, prs := objMap[utils.ID]
	if !prs {
		return "", nil, errors.New("invalid access request format")
	}
	objId := objIdInterface.(string)

	actionInterface, prs := inputMap[utils.ACTION]
	if !prs {
		return "", nil, errors.New("invalid access request format")
	}
	action := actionInterface.(string)

	// query hierarchy
	hierarchy, err := s.queryHierarchy(objId, action)
	if err != nil {
		return "", nil, errors.New("unable to get obj hierarchy")
	}

	var finalRegoPolicy string
	getStoredPolicySucc := false
	//check cache first
	if s.context.HieUseCache {
		value, err := s.context.HierarchyCache.GetIFPresent(hierarchy)
		if err == nil {
			storedRegoPolicy, ok := value.(string)
			if ok {
				finalRegoPolicy = storedRegoPolicy
				fmt.Printf("Hierarchy hit cache!\n")
				getStoredPolicySucc = true
			}
		}
	}

	if !getStoredPolicySucc {
		// assemble policy
		hierarchies := strings.Split(hierarchy, utils.HIERARCHY_SEP)

		assembledPolicy, err := s.assemblePolicy(hierarchies)
		if err != nil {
			return "", nil, errors.New("unable to assemble final rego policy")
		}
		finalRegoPolicy = assembledPolicy
		if s.context.HieUseCache {
			err = s.context.HierarchyCache.SetWithExpire(hierarchy, assembledPolicy,
				time.Duration(s.context.HieCacheTTL)*time.Second)
			if err != nil {
				fmt.Printf("error put hierarchy cache, hierarchy: %v, error: %v\n", hierarchy, err)
			}
		}
	}

	return finalRegoPolicy, inputMap, nil
}

func (s *Server) queryHierarchy(objId string, action string) (string, error) {
	sqlConn, prs := s.context.SqlDB[utils.LOCAL_BASIC_DS]
	if !prs {
		fmt.Printf("unable to find corresponding datasource: %v\n", utils.LOCAL_BASIC_DS)
		return "", errors.New("unable to query hierarchy")
	}

	hierarchyTemp := strings.Replace(utils.HIERARCHY_QUERY_TEMP, utils.TABLENAME, utils.HIERARCHY_TABLE, 1)

	res, err := sqlConn.Query(hierarchyTemp, objId, action)
	//fmt.Printf("query temp: %v, params: %v\n", hierarchyTemp, objId)

	if err != nil {
		fmt.Printf("Unable to execute sql_query, template: %v, params: %v, err: %v\n",
			hierarchyTemp, objId, err)
		return "", errors.New("unable to query hierarchy")
	}

	defer func(res *sql.Rows) {
		err := res.Close()
		if err != nil {
			fmt.Printf("close res err: %v\n", err)
		}
	}(res)

	var hierarchyRes queriedHierarchy

	if res.Next() {
		if err := res.Scan(&hierarchyRes.hierarchy); err != nil {
			fmt.Printf("scan err: %v\n", err)
			return "", errors.New("unable to query hierarchy")
		}
	} else {
		fmt.Printf("empty query result\n")
		return "", errors.New("unable to query hierarchy")
	}

	if hierarchyRes.hierarchy == "" {
		fmt.Printf("empty queried hierarchy\n")
		return "", errors.New("unable to query hierarchy")
	}

	return hierarchyRes.hierarchy, nil
}

func (s *Server) assemblePolicy(hierarchies []string) (string, error) {
	if len(hierarchies) == 0 {
		fmt.Printf("empty hierarchy")
		return "", errors.New("unable to assemble policy")
	}

	sqlConn, prs := s.context.SqlDB[utils.LOCAL_BASIC_DS]
	if !prs {
		fmt.Printf("unable to find corresponding datasource: %v\n", utils.LOCAL_BASIC_DS)
		return "", errors.New("unable to assemble policy")
	}

	// map to store the calculated rule cost
	policyMap := make(map[string][]*ruleWithCost)

	policyQueryTemp := strings.Replace(utils.POLICY_QUERY_TEMP, utils.TABLENAME, utils.POLICY_REPOSITORY_TABLE, 1)
	for _, hie := range hierarchies {
		res, err := sqlConn.Query(policyQueryTemp, hie)

		if err != nil {
			fmt.Printf("Unable to execute sql_query, template: %v, params: %v, err: %v\n",
				policyQueryTemp, hie, err)
			return "", errors.New("unable to assemble policy")
		}

		var policyRes queriedPolicy

		if res.Next() {
			if err := res.Scan(&policyRes.content); err != nil {
				fmt.Printf("scan err: %v\n", err)
				res.Close()
				return "", errors.New("unable to assemble policy")
			}
		} else {
			fmt.Printf("empty query result\n")
			res.Close()
			return "", errors.New("unable to assemble policy")
		}
		res.Close()

		var jsonPolicy []jsonRule

		err = json.Unmarshal([]byte(policyRes.content), &jsonPolicy)
		if err != nil {
			fmt.Printf("fail to parse policy json: %v\n", policyRes.content)
			return "", err
		}

		visited := make(map[string]bool)

		for _, rule := range jsonPolicy {
			if rule.Key == "" {
				fmt.Printf("invalid policy key, policy: %v\n", rule)
				return "", errors.New("invalid policy content")
			}

			_, prsV := visited[rule.Key]
			if !prsV {
				policyMap[rule.Key] = make([]*ruleWithCost, 0)
				visited[rule.Key] = true
			}

			policyArr, prs := policyMap[rule.Key]
			if !prs {
				policyArr = make([]*ruleWithCost, 0)
			}
			policyMap[rule.Key] = append(policyArr, s.computeRuleWithCost(rule.Content))
		}
	}

	finalRegoPolicy := s.parseMapToRego(policyMap)

	return finalRegoPolicy, nil
}

func (s *Server) parseMapToRego(policyMap map[string][]*ruleWithCost) string {
	// assemble real rego policy from its json format
	var sb strings.Builder
	header := `package authz.policy

default PERMIT = false

PERMIT{
`
	sb.WriteString(header)

	// sort the policy with its cost
	sortedKeys := s.sortPolicyKey(policyMap)

	for _, key := range sortedKeys {
		sb.WriteString(fmt.Sprintf("\t%v\n", key))
	}

	sb.WriteString("}\n\n")

	for _, key := range sortedKeys {
		value, _ := policyMap[key]
		for _, rules := range value {
			sb.WriteString(fmt.Sprintf("%v {\n", key))
			for _, rule := range rules.content {
				sb.WriteString(fmt.Sprintf("\t%v\n", rule))
			}
			sb.WriteString("}\n\n")
		}
	}

	fmt.Printf("final policy:\n%v\n\n", sb.String())

	return sb.String()
}

func (s *Server) sortPolicyKey(policyMap map[string][]*ruleWithCost) []string {
	keys := make([]string, len(policyMap))
	ruleCostMap := make(map[string]int64)
	i := 0
	for key, value := range policyMap {
		keys[i] = key
		i++
		var cost int64 = 0
		for _, rule := range value {
			cost = utils.Add(cost, rule.cost)
		}
		sort.SliceStable(value, func(i, j int) bool {
			return value[i].cost < value[j].cost
		})
		ruleCostMap[key] = cost
	}
	sort.SliceStable(keys, func(i, j int) bool {
		costI, _ := ruleCostMap[keys[i]]
		costJ, _ := ruleCostMap[keys[j]]
		return costI < costJ
	})
	fmt.Printf("rule cost table:\n%v\n\n", ruleCostMap)
	return keys
}

func (s *Server) computeRuleWithCost(rule []string) *ruleWithCost {
	var cost int64 = 0
	for _, sentence := range rule {
		if match, err := regexp.MatchString(utils.SQL_QUERY_TERM_REGEX, sentence); err == nil && match {
			datasource := utils.GetFirstTermAfterQuote(sentence, "sql_query(")
			funcKey := "sql_query(" + datasource + ")"
			funcCost, prs := s.context.FuncTimeCounter[funcKey]
			if prs {
				cost = utils.Add(cost, funcCost)
			}
		} else if match, err := regexp.MatchString(utils.API_GET_OBJ_TERM_REGEX, sentence); err == nil && match {
			urlPrefix := utils.GetFirstTermAfterQuote(sentence, "api_get_obj_term(")
			funcKey := "api_get_obj_term(" + urlPrefix + ")"
			funcCost, prs := s.context.FuncTimeCounter[funcKey]
			if prs {
				cost = utils.Add(cost, funcCost)
			}
		}
	}
	return &ruleWithCost{content: rule, cost: cost}
}
