package com.columbia.iotabacserver.controller;

import com.columbia.iotabacserver.pojo.request.*;
import com.columbia.iotabacserver.pojo.response.DevRegResponse;
import com.columbia.iotabacserver.pojo.response.QueryActionsResponse;
import com.columbia.iotabacserver.service.AuthService;
import com.columbia.iotabacserver.service.AuthenticationService;
import com.columbia.iotabacserver.utils.Constants;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import com.columbia.iotabacserver.pojo.response.AuthResponse;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.Resource;

@RestController
public class AbacController {
    private static final Logger logger = LoggerFactory.getLogger(AbacController.class);

    @Resource
    private AuthService authService;

    @Resource
    private AuthenticationService authenticationService;

    @PostMapping(value = "/authz/eval", produces = MediaType.APPLICATION_JSON_VALUE, consumes =
            MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public AuthResponse postEval(@RequestBody AuthRequest request) {
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

        boolean pass = false;
        try {
            pass = authService.opaEval(authService.assembleAccessRequest(request.getSubUsername(),
                    request.getObjDevId(), request.getAction(), request.getEnvInfo()));
        } catch (JsonProcessingException e) {
            logger.info("cannot assemble access request");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Constants.INVALID_ACCESS_REQUEST_INFO);
        }
        if (pass) {
            return new AuthResponse(Constants.TRUE);
        }
        return new AuthResponse(Constants.FALSE);
    }

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

    @PostMapping(value = "authz/login/dev", produces = MediaType.APPLICATION_JSON_VALUE, consumes =
            MediaType.APPLICATION_JSON_VALUE)
    public void postDevLogin(@RequestBody DevLoginRequest request) {
        if (!StringUtils.hasText(request.getDevId()) || !StringUtils.hasText(request.getToken())
                || !authenticationService.deviceAuthenticateCheck(request.getDevId(), request.getToken())) {
            logger.info("invalid device login request");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Constants.INVALID_DEV_AUTHENTICATION);
        }
    }

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

    @PostMapping(value = "authz/login/user", produces = MediaType.APPLICATION_JSON_VALUE, consumes =
            MediaType.APPLICATION_JSON_VALUE)
    public void postUserLogin(@RequestBody UserLoginRequest request) {
        if (!StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword())
                || !authenticationService.userAuthenticateCheck(request.getUsername(), request.getPassword())) {
            logger.info("invalid user login request");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Constants.INVALID_USER_AUTHENTICATION);
        }
    }

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
}
