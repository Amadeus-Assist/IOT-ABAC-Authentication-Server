package com.columbia.iotabacserver.service.impl;

import com.columbia.iotabacserver.pojo.jackson_model.OpaEvalRequestBody;
import com.columbia.iotabacserver.pojo.response.OpaEvalResponse;
import com.columbia.iotabacserver.service.AuthService;
import com.columbia.iotabacserver.utils.Constants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Service
public class AuthServiceImpl implements AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    @Value("${opa_eval_url}")
    private String opaEvalUrl;

    @Override
    public boolean opaEval(String accessRequest) {
        if (!StringUtils.hasText(opaEvalUrl)){
            logger.info("invalid opaEvalUrl: {}", opaEvalUrl);
            return false;
        }

        OpaEvalRequestBody opaEvalRequestBody = new OpaEvalRequestBody(accessRequest);
        String opaEvalBodyJson = null;
        try {
            opaEvalBodyJson = new ObjectMapper().writeValueAsString(opaEvalRequestBody);
        } catch (JsonProcessingException e) {
            logger.info("can't parse opaEvalRequest to json string, request: {}", opaEvalRequestBody);
            return false;
        }

        // send access request and policy to opa server for evaluation
        RestTemplate restTemplate = new RestTemplate();

        //you can create and edit header
        HttpHeaders header = new HttpHeaders();
        // header.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        header.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<String> requestHttp = new HttpEntity<>(opaEvalBodyJson, header);

        //After you can create a request
        ResponseEntity<OpaEvalResponse> response = restTemplate.postForEntity("http://localhost:8081/opa/eval",
                requestHttp,
                OpaEvalResponse.class);

        if (!response.getStatusCode().is2xxSuccessful() || !response.hasBody() || !StringUtils.hasText(response.getBody().getDecision())){
            return false;
        }

        return response.getBody().getDecision().equals(Constants.TRUE);
    }


}
