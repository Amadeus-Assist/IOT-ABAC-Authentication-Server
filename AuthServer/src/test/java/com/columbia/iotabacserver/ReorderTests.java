package com.columbia.iotabacserver;

import com.columbia.iotabacserver.controller.AbacController;
import com.columbia.iotabacserver.dao.mapper.AuthzMapper;
import com.columbia.iotabacserver.dao.model.DevInfoPojo;
import com.columbia.iotabacserver.dao.model.ObjectHierarchyPojo;
import com.columbia.iotabacserver.dao.model.PolicyPojo;
import com.columbia.iotabacserver.pojo.request.AuthRequest;
import com.columbia.iotabacserver.pojo.response.AuthResponse;
import com.columbia.iotabacserver.utils.LocalBeanFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.ResourceUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@SpringBootTest
public class ReorderTests {
    @Test
    public void generateTestDev() throws JsonProcessingException {
        ObjectMapper jsonMapper = new ObjectMapper();
        AuthzMapper mapper = LocalBeanFactory.getBean(AuthzMapper.class);
        Map<String, String> payload = new HashMap<>();
        String devId = "reorder_test_dev";
        payload.put("id", devId);
        DevInfoPojo pojo = new DevInfoPojo(devId, "reorder_dev", "read", "123456",
                jsonMapper.writeValueAsString(payload));
        mapper.insertDevInfoFull(pojo);
        ObjectHierarchyPojo objectHierarchyPojo = new ObjectHierarchyPojo(devId, "read", "reorder_dev");
        mapper.insertObjectHierarchy(objectHierarchyPojo);
    }

    @Test
    public void generateReorderPolicy() throws IOException {
        String reorderPolicy =
                Files.readString(ResourceUtils.getFile("classpath:samples\\reorder\\reorder_policy.txt").toPath());
        AuthzMapper mapper = LocalBeanFactory.getBean(AuthzMapper.class);
        mapper.insertPolicy(new PolicyPojo("reorder_dev", reorderPolicy));
    }

    @Test
    public void testReorder() throws JsonProcessingException {
        ObjectMapper jsonMapper = new ObjectMapper();
        AbacController controller = LocalBeanFactory.getBean(AbacController.class);
        long startTime = System.currentTimeMillis();
        int times = 1;
        for (int i = 0; i < times; i++) {
            Map<String, String> env = new HashMap<>();
            env.put("count", Integer.toString(i));
            String userId = "test_user_1";
            String devId = "reorder_test_dev";
            AuthResponse resp = controller.postEval(new AuthRequest(devId, "123456", userId, "123456", "read",
                    jsonMapper.writeValueAsString(env)));
            System.out.printf("userId: %s%ndevId: %s%ndecision: %s%n------------------------%n", userId, devId,
                    resp.getDecision());
        }
        long duration = System.currentTimeMillis() - startTime;
        System.out.printf("total time: %dms, ave time: %dms%n", duration, duration / times);
    }
}
