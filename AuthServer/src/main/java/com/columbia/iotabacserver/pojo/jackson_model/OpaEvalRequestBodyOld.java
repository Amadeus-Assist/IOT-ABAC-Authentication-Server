package com.columbia.iotabacserver.pojo.jackson_model;

import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class OpaEvalRequestBodyOld {
    private String access_request;
    private String policy;
}
