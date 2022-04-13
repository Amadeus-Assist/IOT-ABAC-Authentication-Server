package com.columbia.iotabacserver.service;

public interface AuthenticationService {
    boolean deviceAuthenticateCheck(String devId, String devToken);

    boolean userAuthenticateCheck(String username, String password);

    boolean deviceExists(String devId);

    String registerDevice(String devId, String devType);

    boolean userExists(String username);

    void registerUser(String username, String password, String attrs);
}
