package com.columbia.iotabacserver.service.impl;

import com.columbia.iotabacserver.dao.mapper.AuthzMapper;
import com.columbia.iotabacserver.dao.model.DevCheckPojo;
import com.columbia.iotabacserver.dao.model.UserCheckPojo;
import com.columbia.iotabacserver.service.AuthenticationService;
import com.columbia.iotabacserver.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationServiceImpl.class);

    @Resource
    private AuthzMapper mapper;

    @Override
    public boolean deviceAuthenticateCheck(String devId, String devToken) {
        DevCheckPojo pojo = mapper.findDevCheckInfo(devId);
        return !ObjectUtils.isEmpty(pojo) && devToken.equals(pojo.getToken());
    }

    @Override
    public boolean userAuthenticateCheck(String username, String password) {
        UserCheckPojo pojo = mapper.findUserCheckInfo(username);
        return !ObjectUtils.isEmpty(pojo) && password.equals(pojo.getPassword());
    }

    @Override
    public boolean deviceExists(String devId) {
        DevCheckPojo pojo = mapper.findDevCheckInfo(devId);
        return !ObjectUtils.isEmpty(pojo);
    }

    @Override
    public String registerDevice(String devId, String devType) {
        String token = Utils.generateNewToken();
        DevCheckPojo pojo = new DevCheckPojo(devId, devType, token);
        mapper.insertDevCheckInfo(pojo);
        return token;
    }
}
