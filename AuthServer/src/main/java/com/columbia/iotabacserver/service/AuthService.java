package com.columbia.iotabacserver.service;

import com.fasterxml.jackson.core.JsonProcessingException;

public interface AuthService {
    // used to evaluate real access request
    boolean opaEval(String accessRequest);

    // used to assemble real access request
    String assembleAccessRequest(String username, String objId, String action, String envInfo) throws JsonProcessingException;
}
