package com.columbia.iotabacserver.pojo.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@ToString
public class QueryActionsRequest {
    @JsonProperty("device_id")
    String devId;
    @JsonProperty("token")
    String token;
}
