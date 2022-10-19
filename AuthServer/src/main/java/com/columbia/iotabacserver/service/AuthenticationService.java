package com.columbia.iotabacserver.service;

public interface AuthenticationService {
    // used to check device authentication info
    boolean deviceAuthenticateCheck(String devId, String devToken);

    // used to check user authentication info
    boolean userAuthenticateCheck(String username, String password);

    // used to check whether device id already exists
    boolean deviceExists(String devId);

    // used to register device
    String registerDevice(String devId, String devType, String attrs);

    // used to check whether username already exists
    boolean userExists(String username);

    // used to register user
    void registerUser(String username, String password, String attrs);

    String queryDevActions(String devId);

    //use to check DB authorization info
    boolean dbAuthorizeCheck(String dbAuthInfo, String[] requiredDB, String userId);
}
