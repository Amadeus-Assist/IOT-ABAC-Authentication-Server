package com.columbia.iotabacserver.pojo.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@ToString
public class UserLoginRequest {
    @JsonProperty("username")
    String username;
    @JsonProperty("password")
    String password;
}
