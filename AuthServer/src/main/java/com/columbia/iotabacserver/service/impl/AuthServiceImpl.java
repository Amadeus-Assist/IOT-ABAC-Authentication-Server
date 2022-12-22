package com.columbia.iotabacserver.service.impl;

import com.columbia.iotabacserver.dao.dbutils.DatabaseOperation;
import com.columbia.iotabacserver.dao.mapper.AuthzMapper;
import com.columbia.iotabacserver.dao.model.UserAttrsPojo;
import com.columbia.iotabacserver.exception.BadRequestException;
import com.columbia.iotabacserver.exception.UnexpectedHttpException;
import com.columbia.iotabacserver.pojo.jackson_model.AccessRequestModel;
import com.columbia.iotabacserver.pojo.jackson_model.OpaEvalRequestBody;
import com.columbia.iotabacserver.pojo.response.OpaEvalResponse;
import com.columbia.iotabacserver.service.AuthService;
import com.columbia.iotabacserver.utils.Constants;
import com.columbia.iotabacserver.utils.Utils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.ErrorHandler;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.columbia.iotabacserver.dao.dbutils.DatabaseOperation;

@Service
public class AuthServiceImpl implements AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    @Value("${opa_eval_url}")
    private String opaEvalUrl;

    @Resource
    private AuthzMapper mapper;

    @Override
    public boolean opaEval(String accessRequest) {
        if (!StringUtils.hasText(opaEvalUrl)) {
            logger.info("invalid opaEvalUrl: {}", opaEvalUrl);
            return false;
        }

        logger.info("access request: {}", accessRequest);

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
        ResponseEntity<OpaEvalResponse> response;
        try {
            response = restTemplate.postForEntity("http://localhost:8081/opa/eval",
                    requestHttp,
                    OpaEvalResponse.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().equals(HttpStatus.BAD_REQUEST)) {
                logger.info("bad request response from opa server: {}", e.getResponseBodyAsString());
            } else {
                logger.info("raise HttpClientErrorException: {}", e.getStackTrace()[0]);
            }
            return false;
        }

        logger.info("eval response: {}", response);

        logger.info("eval response body: {}", response.getBody());

        if (!response.getStatusCode().is2xxSuccessful() || !response.hasBody() || !StringUtils.hasText(response.getBody().getDecision())) {
            return false;
        }

        return response.getBody().getDecision().equals(Constants.TRUE);
    }

    @Override
    public String assembleAccessRequest(String username, String objId, String action, String envInfo) throws JsonProcessingException {
        UserAttrsPojo pojo = new UserAttrsPojo();
        try{
            pojo = DatabaseOperation.findUserAttrs(username);
        } catch (IOException e) {
            logger.info("cannot assemble access request: {}", e.toString());
        }
        Map<?, ?> subAttrsMap = Utils.jacksonMapper.readValue(pojo.getAttrs(),
                HashMap.class);
        // Map<?, ?> subAttrsMap = Utils.jacksonMapper.readValue(mapper.findUserAttrs(username).getAttrs(),
        //         HashMap.class);
        Map<?, ?> objAttrsMap = Utils.jacksonMapper.readValue(mapper.findDevAttrs(objId).getAttrs(), HashMap.class);
        Map<?, ?> envInfoMap = Utils.jacksonMapper.readValue(envInfo, HashMap.class);
        AccessRequestModel requestModel = new AccessRequestModel(subAttrsMap, objAttrsMap, action, envInfoMap);
        return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(requestModel);
    }


}
