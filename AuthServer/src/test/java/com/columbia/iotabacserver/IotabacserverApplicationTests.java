package com.columbia.iotabacserver;

import com.columbia.iotabacserver.controller.AbacController;
import com.columbia.iotabacserver.dao.mapper.AuthzMapper;
import com.columbia.iotabacserver.dao.model.DevCheckPojo;
import com.columbia.iotabacserver.dao.model.ObjectHierarchyPojo;
import com.columbia.iotabacserver.dao.model.PolicyPojo;
import com.columbia.iotabacserver.dao.model.UserAttrsPojo;
import com.columbia.iotabacserver.pojo.jackson_model.OpaEvalRequestBody;
import com.columbia.iotabacserver.pojo.jackson_model.OpaEvalRequestBodyOld;
import com.columbia.iotabacserver.pojo.jackson_model.RuleJsonModel;
import com.columbia.iotabacserver.pojo.request.AuthRequest;
import com.columbia.iotabacserver.pojo.request.AuthRequestSecure;
import com.columbia.iotabacserver.pojo.request.DbAuthRequest;
import com.columbia.iotabacserver.pojo.response.AuthResponse;
import com.columbia.iotabacserver.pojo.response.OpaEvalResponse;
import com.columbia.iotabacserver.utils.Utils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ResourceUtils;
import com.columbia.iotabacserver.utils.LocalBeanFactory;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

@SpringBootTest
class IotabacserverApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void insertPolicy() {
        AuthzMapper mapper = LocalBeanFactory.getBean(AuthzMapper.class);
        String policyRef = "door_sample";
        String contents = null;
        try {
            File file = ResourceUtils.getFile("classpath:samples\\door_sample.txt");
            contents = Files.readString(file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        PolicyPojo pojo = new PolicyPojo(policyRef, contents);
        mapper.insertPolicy(pojo);
    }

    @Test
    void updatePolicy() {
        AuthzMapper mapper = LocalBeanFactory.getBean(AuthzMapper.class);
        String policyRef = "door_columbia_seas_office";
        String contents = null;
        try {
            File file = ResourceUtils.getFile("classpath:samples\\door_columbia_seas_office.txt");
            contents = Files.readString(file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        PolicyPojo pojo = new PolicyPojo(policyRef, contents);
        mapper.updatePolicy(pojo);
    }

    @Test
    void insertHierarchy() {
        AuthzMapper mapper = LocalBeanFactory.getBean(AuthzMapper.class);
        String objId = "door_sample";
        String action = "open";
        String hierarchy = "door_sample";
        ObjectHierarchyPojo pojo = new ObjectHierarchyPojo(objId, action, hierarchy);
        mapper.insertObjectHierarchy(pojo);
    }

    @Test
    void insertUserAttrs() {
        AuthzMapper mapper = LocalBeanFactory.getBean(AuthzMapper.class);
        String userId = "david_7155";
        String attrs = null;
        try {
            File file = ResourceUtils.getFile("classpath:samples\\david_attrs.txt");
            attrs = Files.readString(file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        UserAttrsPojo pojo = new UserAttrsPojo(userId, "123456", attrs);
        mapper.insertUserAttrs(pojo);
    }

    @Test
    void updateUserAttrs() {
        AuthzMapper mapper = LocalBeanFactory.getBean(AuthzMapper.class);
        String userId = "alice_5832";
        String attrs = null;
        try {
            File file = ResourceUtils.getFile("classpath:samples\\alice_attrs.txt");
            attrs = Files.readString(file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        UserAttrsPojo pojo = new UserAttrsPojo(userId, "123456", attrs);
        mapper.updateUserAttrs(pojo);
    }

    @Test
    void basicEval() throws JsonProcessingException {
        for (int i = 1; i < 3; i++) {
            // get and parse the access request
            String arFile = "classpath:samples\\access_request_alice.txt";
            String arContent = null;
            try {
                File file = ResourceUtils.getFile(arFile);
                arContent = Files.readString(file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException();
            }
            JsonNode arRoot = new ObjectMapper().readTree(arContent);
            JsonNode objIdNode = arRoot.path("obj").path("id");
            if (!objIdNode.isTextual()) {
                throw new RuntimeException();
            }
            String objId = objIdNode.asText();

            JsonNode actionNode = arRoot.path("action");
            if (!actionNode.isTextual()) {
                throw new RuntimeException();
            }
            String action = actionNode.asText();

            // query database to retrieve policy hierarchy
            AuthzMapper mapper = LocalBeanFactory.getBean(AuthzMapper.class);
            ObjectHierarchyPojo hierarchyPojo = mapper.findHierarchy(objId, action);
            String[] hierarchy = hierarchyPojo.getHierarchy().split("/");

            // retrieve policies from database and assemble
            Map<String, List<List<String>>> assembledPolicyMap = new HashMap<>();
            for (String policyRef : hierarchy) {
                PolicyPojo pojo = mapper.findPolicy(policyRef);
                System.out.printf("Policy Ref: %s%n%s%n%n", pojo.getRef(), pojo.getContent());
                List<RuleJsonModel> ruleModels = new ObjectMapper().readValue(pojo.getContent(), new TypeReference<>() {
                });
                Set<String> localKey = new HashSet<>();
                for (RuleJsonModel ruleModel : ruleModels) {
                    String key = ruleModel.getKey();
                    if (!localKey.contains(key)) {
                        assembledPolicyMap.put(key, new ArrayList<>());
                        localKey.add(key);
                    }
                    assembledPolicyMap.get(key).add(ruleModel.getContent());
                }
            }

            System.out.printf("Final assembled policies:%n%s%n%n", assembledPolicyMap.toString());

            // parse the assembled policy map to rego policy
            StringBuilder sb = new StringBuilder();
            sb.append("package authz.policy\n\ndefault PERMIT = false\n\nPERMIT {\n");

            List<String> ruleKeys = new ArrayList<>(assembledPolicyMap.keySet());
            ruleKeys.sort(Comparator.naturalOrder());
            for (String key : ruleKeys) {
                sb.append("\t").append(key).append("\n");
            }
            sb.append("}\n\n");

            for (String key : ruleKeys) {
                List<List<String>> ruleList = assembledPolicyMap.get(key);
                for (List<String> rule : ruleList) {
                    sb.append(key).append(" {\n");
                    for (String sentence : rule) {
                        sb.append("\t").append(sentence).append("\n");
                    }
                    sb.append("}\n");
                }
            }

            String finalRegoPolicy = sb.toString();
            System.out.printf("Final rego policy: %n%s%n", finalRegoPolicy);

            OpaEvalRequestBodyOld opaEvalRequestBodyOld = new OpaEvalRequestBodyOld(arContent, finalRegoPolicy);
            String opaEvalBodyJson = new ObjectMapper().writeValueAsString(opaEvalRequestBodyOld);

            // send access request and policy to opa server for evaluation
            RestTemplate restTemplate = new RestTemplate();

            //you can create and edit header
            HttpHeaders header = new HttpHeaders();
            // header.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            header.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            HttpEntity<String> requestHttp = new HttpEntity<String>(opaEvalBodyJson, header);

//        System.out.println(requestHttp);

            //After you can create a request
            ResponseEntity<OpaEvalResponse> response = restTemplate.postForEntity("http://localhost:8081/opa/eval",
                    requestHttp,
                    OpaEvalResponse.class);

            System.out.println("returned decision: " + response.getBody().getDecision());
        }

//        Map<String, Object> arMap = new ObjectMapper().readValue(arContent, new TypeReference<>(){});
//        if(!arMap.containsKey("sub")){
//            throw new RuntimeException();
//        }
//        Object subObj = arMap.get("sub");
//        if (subObj instanceof Map){
//            Map<String, Object> subMap = (Map<String, Object>) subObj;
//            if(subMap.containsKey("id")){
//                Object deviceIdObj = subMap.get("id");
//                if(deviceIdObj instanceof String){
//                    String deviceId = (String) deviceIdObj;
//                    System.out.println(deviceId);
//                }
//            }else {
//                throw new RuntimeException();
//            }
//        }
    }

    @Test
    void testNewBasicEval() throws JsonProcessingException {
        for (int i = 0; i < 2; i++) {
            // get and parse the access request
            String arFile = "classpath:samples\\access_request_alice.txt";
            String arContent = null;
            try {
                File file = ResourceUtils.getFile(arFile);
                arContent = Files.readString(file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException();
            }

            OpaEvalRequestBody opaEvalRequestBody = new OpaEvalRequestBody(arContent);
            String opaEvalBodyJson = new ObjectMapper().writeValueAsString(opaEvalRequestBody);

            // send access request and policy to opa server for evaluation
            RestTemplate restTemplate = new RestTemplate();

            //you can create and edit header
            HttpHeaders header = new HttpHeaders();
            // header.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            header.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            HttpEntity<String> requestHttp = new HttpEntity<String>(opaEvalBodyJson, header);

            //After you can create a request
            ResponseEntity<OpaEvalResponse> response = restTemplate.postForEntity("http://localhost:8081/opa/eval",
                    requestHttp,
                    OpaEvalResponse.class);

            System.out.println("returned decision: " + response.getBody().getDecision());
        }
    }

    @Test
    void testMybatis(){
        AuthzMapper mapper = LocalBeanFactory.getBean(AuthzMapper.class);
        DevCheckPojo pojo = mapper.findDevCheckInfo("123");
        System.out.println("pojo: "+pojo);
    }

    @Test
    void testGenerateToken(){
        System.out.println(Utils.generateNewToken());
    }

    @Test
    void testEval() throws JsonProcessingException {
        AbacController controller = LocalBeanFactory.getBean(AbacController.class);
        // get and parse the access request
        String arFile = "classpath:samples\\new_access_request.txt";
        String arContent = null;
        try {
            File file = ResourceUtils.getFile(arFile);
            arContent = Files.readString(file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }

        ObjectMapper mapper = new ObjectMapper();
        AuthRequest request = mapper.readValue(arContent, AuthRequest.class);
        AuthResponse response = controller.postEval(request);
        System.out.printf("decision: %s\n", response.getDecision());
    }

    @Test
    void testDBAuth() throws JsonProcessingException {
        AbacController controller = LocalBeanFactory.getBean(AbacController.class);
        String arFile = "classpath:samples\\authorize_aster_1.txt";
        String arContent = null;
        try {
            File file = ResourceUtils.getFile(arFile);
            arContent = Files.readString(file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
        ObjectMapper mapper = new ObjectMapper();
        AuthRequestSecure request = mapper.readValue(arContent, AuthRequestSecure.class);
        // 2nd time req
        AuthResponse response = controller.postEvalSecure(request);
        System.out.printf("decision: %s\n", response.getDecision());
    }

    @Test
    void testInsertPermInfo() throws JsonProcessingException {
        AuthzMapper mapper = LocalBeanFactory.getBean(AuthzMapper.class);
        String userId = "Bob";
        String tableName = "user_attrs";
        String startTime = "1989-06-04";
        mapper.insertPermInfo(userId, tableName, startTime);
        System.out.printf("Inserted row for %s %s\n", userId, tableName);
    }
}
