package com.columbia.iotabacserver.service;

import com.fasterxml.jackson.core.JsonProcessingException;

public interface AuthService {
    boolean opaEval(String accessRequest);

    String assembleAccessRequest(String username, String objId, String action, String envInfo) throws JsonProcessingException;
}
