package com.columbia.iotabacserver.pojo.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@ToString

public class AuthRequestSecure {
    @JsonProperty("obj_dev_id")
    private String objDevId;
    @JsonProperty("obj_token")
    private String objToken;
    @JsonProperty("sub_username")
    private String subUsername;
    @JsonProperty("sub_user_password")
    private String subUserPwd;
    @JsonProperty("action")
    private String action;
    @JsonProperty("env_info")
    private String envInfo;
    @JsonProperty("secured")
    private String secured;
    @JsonProperty("dbAuth")
    private String dbauth;
}
