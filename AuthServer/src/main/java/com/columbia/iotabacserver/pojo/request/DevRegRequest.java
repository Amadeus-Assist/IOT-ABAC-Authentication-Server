package com.columbia.iotabacserver.pojo.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@ToString
public class DevRegRequest {
    @JsonProperty("device_id")
    private String devId;
    @JsonProperty("device_type")
    private String devType;
    @JsonProperty("attrs")
    private String attrs;
}
