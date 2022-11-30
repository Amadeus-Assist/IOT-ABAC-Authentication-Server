package com.columbia.iotabacserver.service.impl;

import com.columbia.iotabacserver.dao.mapper.AuthzMapper;
import com.columbia.iotabacserver.dao.model.*;
import com.columbia.iotabacserver.service.AuthenticationService;
import com.columbia.iotabacserver.utils.Constants;
import com.columbia.iotabacserver.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;

import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.HashSet;

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
    public String registerDevice(String devId, String devType, String attrs) {
        String token = Utils.generateNewToken();
        DevRegisterPojo pojo = new DevRegisterPojo(devId, devType, token, attrs);
        mapper.insertDevInfo(pojo);
        return token;
    }

    @Override
    public boolean userExists(String username) {
        UserCheckPojo pojo = mapper.findUserCheckInfo(username);
        return !ObjectUtils.isEmpty(pojo);
    }

    @Override
    public void registerUser(String username, String password, String attrs) {
        mapper.insertUserAttrs(new UserAttrsPojo(username, password, attrs));
    }

    @Override
    public String queryDevActions(String devId) {
        DevActionsPojo pojo = mapper.findDevActions(devId);
        if (ObjectUtils.isEmpty(pojo)) {
            return "";
        }
        return StringUtils.hasText(pojo.getActions()) ? pojo.getActions() : "";
    }

    private Integer getLength(String perm) {
        HashMap<String, Integer> converter = new HashMap<String, Integer>(){{
            put("always", 99999);
            put("30", 30);
            put("7", 7);
            put("once", 0);
        }};
        for(String length: converter.keySet()) {
            if(perm.contains(length)) return converter.get(length);
        }
        return -1;
    }

    @Override 
    public boolean dbAuthorizeCheck(String dbAuthInfo, String[] requiredDB, String userId) {
        DBAuthInfoPojo authInfo = new DBAuthInfoPojo(dbAuthInfo);
        HashMap<String, String> dbAuthInfoMap = authInfo.getDbAuthInfoMap();
        boolean flag = true;
        HashSet<String> secureDB = new HashSet<String>();
        secureDB.add("user_attrs");
        for(String table:requiredDB){
            if(!secureDB.contains(table)) continue;//pass if not secure DB

            DBAccessPermPojo pojo = mapper.findAccessDate(userId, table);
            if(pojo.getAllowDate()!= null && LocalDate.parse(pojo.getAllowDate()).compareTo(LocalDate.now()) >= 0) continue; // has record

            if(!dbAuthInfoMap.containsKey(table)) {
                return false;
            }

            String perm = dbAuthInfoMap.get(table);
            if(perm.contains(Constants.ALLOW) ){
                if(!perm.contains(Constants.ONCE)) {
                    pojo.setAllowLength(getLength(perm));
                    pojo.setTableName(table);
                    pojo.setAllowDate((LocalDate.now().plusDays(pojo.getAllowLength())).toString());
                    mapper.updateSecureDBAllow(pojo);
                }
            }

            else if(perm.contains(Constants.DENY)) {
                flag = false;
                if(!perm.contains(Constants.ONCE)) {
                    pojo.setDenyLength(getLength(perm));
                    pojo.setTableName(table);
                    pojo.setDenyDate((LocalDate.now().plusDays(pojo.getDenyLength())).toString());
                    mapper.updateSecureDBDeny(pojo);//bugging
                }
            }
        }
        return flag;
    }

}
