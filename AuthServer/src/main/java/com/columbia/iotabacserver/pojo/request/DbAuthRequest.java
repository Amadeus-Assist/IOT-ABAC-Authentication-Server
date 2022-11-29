package com.columbia.iotabacserver.pojo.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@ToString
public class DbAuthRequest {
    @JsonProperty("valid")
    private boolean valid = false;
    @JsonProperty("db_auth")
    private String dbAuth = "";
    @JsonProperty("db_password")
    private String dbPassword = "";
}