package com.columbia.iotabacserver.controller;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.columbia.iotabacserver.pojo.request.AuthRequest;
import com.columbia.iotabacserver.pojo.request.DbAuthRequest;
import com.columbia.iotabacserver.pojo.request.DevLoginRequest;
import com.columbia.iotabacserver.pojo.request.DevRegRequest;
import com.columbia.iotabacserver.pojo.request.QueryActionsRequest;
import com.columbia.iotabacserver.pojo.request.UserLoginRequest;
import com.columbia.iotabacserver.pojo.request.UserRegRequest;
import com.columbia.iotabacserver.pojo.response.AuthResponse;
import com.columbia.iotabacserver.pojo.response.DevRegResponse;
import com.columbia.iotabacserver.pojo.response.QueryActionsResponse;
import com.columbia.iotabacserver.service.AuthService;
import com.columbia.iotabacserver.service.AuthenticationService;
import com.columbia.iotabacserver.utils.Constants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.columbia.iotabacserver.pojo.request.AuthRequestSecure;

import com.columbia.iotabacserver.controller.AbacController;
import com.columbia.iotabacserver.dao.mapper.AuthzMapper;
import com.columbia.iotabacserver.dao.model.DevCheckPojo;
import com.columbia.iotabacserver.dao.model.ObjectHierarchyPojo;
import com.columbia.iotabacserver.dao.model.PolicyPojo;
import com.columbia.iotabacserver.dao.model.UserAttrsPojo;
import com.columbia.iotabacserver.dao.model.DBAccessPermPojo;
import com.columbia.iotabacserver.pojo.jackson_model.OpaEvalRequestBody;
import com.columbia.iotabacserver.pojo.jackson_model.OpaEvalRequestBodyOld;
import com.columbia.iotabacserver.pojo.jackson_model.RuleJsonModel;
import com.columbia.iotabacserver.pojo.response.OpaEvalResponse;
import com.columbia.iotabacserver.utils.Utils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ResourceUtils;
import com.columbia.iotabacserver.utils.LocalBeanFactory;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
@RestController
public class AbacController {
    private static final Logger logger = LoggerFactory.getLogger(AbacController.class);

    @Resource
    private AuthService authService;

    @Resource
    private AuthenticationService authenticationService;

    // url to receive evaluate request from client
    @PostMapping(value = "/authz/eval", produces = MediaType.APPLICATION_JSON_VALUE, consumes =
            MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public AuthResponse postEval(@RequestBody AuthRequest request) {
        String[] required_tables = {"user_attrs"};
        boolean needSecureDB = needSecureDB(request, required_tables);
        // check necessary info not empty and authentication info correct
        if (!StringUtils.hasText(request.getSubUsername()) || !StringUtils.hasText(request.getSubUserPwd())
                || !authenticationService.userAuthenticateCheck(request.getSubUsername(), request.getSubUserPwd())) {
            logger.info("invalid user authentication info");    
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Constants.INVALID_USER_INFO);
        }

        if (!StringUtils.hasText(request.getObjDevId()) || !StringUtils.hasText(request.getObjToken())
                || !authenticationService.deviceAuthenticateCheck(request.getObjDevId(), request.getObjToken())) {
            logger.info("invalid device authentication info");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Constants.INVALID_DEV_INFO);
        }

        if (!StringUtils.hasText(request.getAction()) || !StringUtils.hasText(request.getEnvInfo())) {
            logger.info("empty access request");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Constants.INVALID_ACCESS_REQUEST_INFO);
        }

        if(needSecureDB) { 
            AuthzMapper mapper = LocalBeanFactory.getBean(AuthzMapper.class);
            for(String table: required_tables){
                if(mapper.findDenyDate(request.getSubUsername(), table) == null) {
                    mapper.insertPermInfo(request.getSubUsername(), table, Constants.DEFAULT_TIME);
                }
                DBAccessPermPojo pojo = mapper.findDenyDate(request.getSubUsername(), table);
                if(LocalDate.now().compareTo(LocalDate.parse(pojo.getDenyDate())) <= 0) {
                    logger.info("cannot gain access to DB");
                    return new AuthResponse(Constants.FALSE);
                }
            }
            return new AuthResponse(Constants.DK);
        }
        
        boolean pass = false;//don't need private db access
        try {
            // assemble the real access request and forward to OpaServer
            pass = authService.opaEval(authService.assembleAccessRequest(request.getSubUsername(),
                    request.getObjDevId(), request.getAction(), request.getEnvInfo()));
        } catch (JsonProcessingException e) {
            logger.info("cannot assemble access request: {}", e.toString());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Constants.INVALID_ACCESS_REQUEST_INFO);
        }
        if (pass) {
            return new AuthResponse(Constants.TRUE);
        }
        return new AuthResponse(Constants.FALSE);
    }

    @PostMapping(value = "/authz/evalsecure", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    
    @ResponseBody
    public AuthResponse postEvalSecure(@RequestBody AuthRequestSecure request) {
        // check necessary info not empty and authentication info correct
        if (!StringUtils.hasText(request.getSubUsername()) || !StringUtils.hasText(request.getSubUserPwd())
                || !authenticationService.userAuthenticateCheck(request.getSubUsername(), request.getSubUserPwd())) {
            logger.info("invalid user authentication info");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Constants.INVALID_USER_INFO);
        }

        if (!StringUtils.hasText(request.getObjDevId()) || !StringUtils.hasText(request.getObjToken())
                || !authenticationService.deviceAuthenticateCheck(request.getObjDevId(), request.getObjToken())) {
            logger.info("invalid device authentication info");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Constants.INVALID_DEV_INFO);
        }

        if (!StringUtils.hasText(request.getAction()) || !StringUtils.hasText(request.getEnvInfo())) {
            logger.info("empty access request");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Constants.INVALID_ACCESS_REQUEST_INFO);
        }
        
        String[] requiredDB = new String[]{"user_attrs"}; //load the required DBs
        return postEvalDBAuth(request, requiredDB);        
    }
    //handle secure evaluation
    public AuthResponse postEvalDBAuth(AuthRequestSecure request, String[] requiredDB) {

        if(!authenticationService.dbAuthorizeCheck(request.getDbauth(), requiredDB, request.getSubUsername())) { 
            logger.info("cannot gain access to DB");
            return new AuthResponse(Constants.FALSE);
        }

        boolean pass = false;
        try {
            // assemble the real access request and forward to OpaServer
            pass = authService.opaEval(authService.assembleAccessRequest(request.getSubUsername(),
                    request.getObjDevId(), request.getAction(), request.getEnvInfo()));
        } catch (JsonProcessingException e) {
            logger.info("cannot assemble access request: {}", e.toString());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Constants.INVALID_ACCESS_REQUEST_INFO);
        }
        if (pass) {
            return new AuthResponse(Constants.TRUE);
        }
        return new AuthResponse(Constants.FALSE);

    }
    // handle device registration
    @PostMapping(value = "/authz/register/dev", produces = MediaType.APPLICATION_JSON_VALUE, consumes =
            MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public DevRegResponse postDevRegister(@RequestBody DevRegRequest request) {
        if (!StringUtils.hasText(request.getDevId()) || !StringUtils.hasText(request.getDevType())
                || !StringUtils.hasText(request.getAttrs())) {
            logger.info("invalid device register request");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Constants.INVALID_DEV_REG_INFO);
        }

        if (authenticationService.deviceExists(request.getDevId())) {
            logger.info("device already registered");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Constants.DEV_ALREADY_REG);
        }

        String token = authenticationService.registerDevice(request.getDevId(), request.getDevType(),
                request.getAttrs());
        return new DevRegResponse(token);
    }


    // handle device login
    @PostMapping(value = "authz/login/dev", produces = MediaType.APPLICATION_JSON_VALUE, consumes =
            MediaType.APPLICATION_JSON_VALUE)
    public void postDevLogin(@RequestBody DevLoginRequest request) {
        if (!StringUtils.hasText(request.getDevId()) || !StringUtils.hasText(request.getToken())
                || !authenticationService.deviceAuthenticateCheck(request.getDevId(), request.getToken())) {
            logger.info("invalid device login request");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Constants.INVALID_DEV_AUTHENTICATION);
        }
    }


    // handle user registration
    @PostMapping(value = "authz/register/user", produces = MediaType.APPLICATION_JSON_VALUE, consumes =
            MediaType.APPLICATION_JSON_VALUE)
    public void postUserRegister(@RequestBody UserRegRequest request) {
        if (!StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword())
                || !StringUtils.hasText(request.getAttrs())) {
            logger.info("invalid user register request");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Constants.INVALID_USER_REG_INFO);
        }

        if (authenticationService.userExists(request.getUsername())) {
            logger.info("user already registered");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Constants.USER_ALREADY_REG);
        }

        authenticationService.registerUser(request.getUsername(), request.getPassword(), request.getAttrs());
    }


    // handle user login
    @PostMapping(value = "authz/login/user", produces = MediaType.APPLICATION_JSON_VALUE, consumes =
            MediaType.APPLICATION_JSON_VALUE)
    public void postUserLogin(@RequestBody UserLoginRequest request) {
        if (!StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword())
                || !authenticationService.userAuthenticateCheck(request.getUsername(), request.getPassword())) {
            logger.info("invalid user login request");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Constants.INVALID_USER_AUTHENTICATION);
        }
    }


    // handle action query request
    @PostMapping(value = "authz/query-actions/dev", produces = MediaType.APPLICATION_JSON_VALUE, consumes =
            MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public QueryActionsResponse getDeviceActions(@RequestBody QueryActionsRequest request) {
        if (!StringUtils.hasText(request.getDevId()) || !StringUtils.hasText(request.getToken())
                || !authenticationService.deviceAuthenticateCheck(request.getDevId(), request.getToken())) {
            logger.info("invalid device actions query request");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Constants.INVALID_DEV_AUTHENTICATION);
        }
        String actions = authenticationService.queryDevActions(request.getDevId());
        return new QueryActionsResponse(actions);
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<String> handleResponseStatusException(ResponseStatusException e) {
        return new ResponseEntity<>(e.getMessage(), e.getStatus());
    }

    //convert request with secure db to JWT token string
    public String jwtToken(String user, String password) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(password);
            String token = JWT.create()
                .withIssuer("auth0")
                .sign(algorithm);
            return token;
        } catch (JWTCreationException exception){
            throw new JWTCreationException("JWT creation failed", exception);
        }
    }

    public boolean needSecureDB(AuthRequest request, String[] requiredDB) {
        boolean flag = false;
        AuthzMapper mapper = LocalBeanFactory.getBean(AuthzMapper.class);
        for(String table:requiredDB){
            System.out.println(request.getSubUsername()+ ": " + table);
            DBAccessPermPojo pojo = mapper.findAccessDate(request.getSubUsername(), table);
            System.out.println("found record: " + pojo.getAllowDate());
            if(LocalDate.parse(pojo.getAllowDate()).compareTo(LocalDate.now()) >= 0) {
                System.out.println("condition satisfied");
            }
            else {
                flag = true;
            }
        }
        return flag;
    }

    public boolean needSecureDB2(AuthRequestSecure request, String[] requiredDB) {
        boolean flag = false;
        AuthzMapper mapper = LocalBeanFactory.getBean(AuthzMapper.class);
        for(String table:requiredDB){
            System.out.println(request.getSubUsername()+ ": " + table);
            DBAccessPermPojo pojo = mapper.findAccessDate(request.getSubUsername(), table);
            System.out.println("found record: " + pojo.getAllowDate());
            if(LocalDate.parse(pojo.getAllowDate()).compareTo(LocalDate.now()) >= 0){
                System.out.println("satisfied");
            }
            else {
                flag = true;
            }
        }
        return flag;
    }
}
