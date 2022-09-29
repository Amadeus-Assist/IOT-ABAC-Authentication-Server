package com.columbia.iotabacserver;

import com.columbia.iotabacserver.controller.AbacController;
import com.columbia.iotabacserver.dao.mapper.AuthzMapper;
import com.columbia.iotabacserver.dao.model.DevInfoPojo;
import com.columbia.iotabacserver.dao.model.ObjectHierarchyPojo;
import com.columbia.iotabacserver.dao.model.PolicyPojo;
import com.columbia.iotabacserver.dao.model.UserAttrsPojo;
import com.columbia.iotabacserver.pojo.request.AuthRequest;
import com.columbia.iotabacserver.pojo.response.AuthResponse;
import com.columbia.iotabacserver.utils.LocalBeanFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.ResourceUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@SpringBootTest
public class AttributeCacheTests {
    @Test
    public void generateTestUsers() throws JsonProcessingException {
        ObjectMapper jsonMapper = new ObjectMapper();
        AuthzMapper mapper = LocalBeanFactory.getBean(AuthzMapper.class);
        for (int i = 1; i <= 10; i++) {
            Map<String, String> payload = new HashMap<>();
            payload.put("id", "test_user_" + i);
            payload.put("class", Integer.toString(new Random().nextInt(10)));
            UserAttrsPojo pojo = new UserAttrsPojo("test_user_" + i, "123456", jsonMapper.writeValueAsString(payload));
            mapper.insertUserAttrs(pojo);
        }
    }

    @Test
    public void generateTestSQLDevs() throws JsonProcessingException {
        ObjectMapper jsonMapper = new ObjectMapper();
        AuthzMapper mapper = LocalBeanFactory.getBean(AuthzMapper.class);
        for (int i = 10000; i <= 10000; i++) {
            Map<String, String> payload = new HashMap<>();
            String devId = "sql_dev_" + i;
            payload.put("id", devId);
            payload.put("class", Integer.toString(new Random().nextInt(10)));
            DevInfoPojo pojo = new DevInfoPojo(devId, "sql_dev", "read", null,
                    jsonMapper.writeValueAsString(payload));
            mapper.insertDevInfoFull(pojo);
            ObjectHierarchyPojo objectHierarchyPojo = new ObjectHierarchyPojo(devId, "read", "test_sql_dev");
            mapper.insertObjectHierarchy(objectHierarchyPojo);
        }
    }

    @Test
    public void generateTestApiDevs() throws JsonProcessingException {
        ObjectMapper jsonMapper = new ObjectMapper();
        AuthzMapper mapper = LocalBeanFactory.getBean(AuthzMapper.class);
        for (int i = 1; i <= 10000; i++) {
            Map<String, String> payload = new HashMap<>();
            String devId = "api_dev_" + i;
            payload.put("id", devId);
            payload.put("class", Integer.toString(new Random().nextInt(10)));
            DevInfoPojo pojo = new DevInfoPojo(devId, "api_dev", "read", null,
                    jsonMapper.writeValueAsString(payload));
            mapper.insertDevInfoFull(pojo);
            ObjectHierarchyPojo objectHierarchyPojo = new ObjectHierarchyPojo(devId, "read", "test_api_dev");
            mapper.insertObjectHierarchy(objectHierarchyPojo);
        }
    }

    @Test
    public void generatePolicies() throws IOException {
        String sql_dev_policy = Files.readString(ResourceUtils.getFile("classpath:samples\\test_sql_dev.txt").toPath());
        String api_dev_policy = Files.readString(ResourceUtils.getFile("classpath:samples\\test_api_dev.txt").toPath());
        AuthzMapper mapper = LocalBeanFactory.getBean(AuthzMapper.class);
        mapper.insertPolicy(new PolicyPojo("test_sql_dev", sql_dev_policy));
        mapper.insertPolicy(new PolicyPojo("test_api_dev", api_dev_policy));
    }

    @Test
    public void testAuth() {
        AbacController controller = LocalBeanFactory.getBean(AbacController.class);
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 5000; i++) {
            String userId = "test_user_" + (new Random().nextInt(100) + 1);
            Random r = new Random();
            int devType = r.nextInt(2);
            String devPrefix;
            if (devType == 0) {
                devPrefix = "sql_dev_";
            } else {
                devPrefix = "api_dev_";
            }
            String devId = devPrefix + (r.nextInt(100) + 1);
            AuthResponse resp = controller.postEval(new AuthRequest(devId, "123456", userId, "123456", "read", "{}"));
            System.out.printf("userId: %s%ndevId: %s%ndecision: %s%n------------------------%n", userId, devId,
                    resp.getDecision());
        }
        long duration = System.currentTimeMillis() - startTime;
        System.out.printf("total time: %d ms, ave time: %d ms%n", duration, duration / 5000);
    }
}
