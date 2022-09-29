package com.columbia.iotabacserver;

import com.columbia.iotabacserver.controller.AbacController;
import com.columbia.iotabacserver.dao.mapper.AuthzMapper;
import com.columbia.iotabacserver.dao.model.DevInfoPojo;
import com.columbia.iotabacserver.dao.model.PolicyPojo;
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

@SpringBootTest
public class PolicyCacheTests {
    @Test
    public void generatePolicies() throws IOException {
        String hie1 =
                Files.readString(ResourceUtils.getFile("classpath:samples\\policycache\\test_hie_1.txt").toPath());
        String hie2 =
                Files.readString(ResourceUtils.getFile("classpath:samples\\policycache\\test_hie_2.txt").toPath());
        String hie3 =
                Files.readString(ResourceUtils.getFile("classpath:samples\\policycache\\test_hie_3.txt").toPath());
        AuthzMapper mapper = LocalBeanFactory.getBean(AuthzMapper.class);
        mapper.insertPolicy(new PolicyPojo("test_hie_1", hie1));
        mapper.insertPolicy(new PolicyPojo("test_hie_2", hie2));
        mapper.insertPolicy(new PolicyPojo("test_hie_3", hie3));
    }

    @Test
    public void generateHieTestObj() throws JsonProcessingException {
        ObjectMapper jsonMapper = new ObjectMapper();
        AuthzMapper mapper = LocalBeanFactory.getBean(AuthzMapper.class);
        String devId = "hie_test_dev";
        Map<String, String> payload = new HashMap<>();
        payload.put("id", devId);
        mapper.insertDevInfoFull(new DevInfoPojo(devId, "hie_test", "read", "123456",
                jsonMapper.writeValueAsString(payload)));
    }

    @Test
    public void testHieCache(){
        AbacController controller = LocalBeanFactory.getBean(AbacController.class);
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 5000; i++) {
            String userId = "test_user_1";
            String devId = "hie_test_dev";
            AuthResponse resp = controller.postEval(new AuthRequest(devId, "123456", userId, "123456", "read", "{}"));
            System.out.printf("userId: %s%ndevId: %s%ndecision: %s%n------------------------%n", userId, devId,
                    resp.getDecision());
        }
        long duration = System.currentTimeMillis() - startTime;
        System.out.printf("total time: %d ms, ave time: %d ms%n", duration, duration / 5000);
    }
}
