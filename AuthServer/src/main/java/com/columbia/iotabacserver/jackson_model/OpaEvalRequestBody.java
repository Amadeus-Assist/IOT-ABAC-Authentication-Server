package com.columbia.iotabacserver.jackson_model;

import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class OpaEvalRequestBody {
    private String access_request;
    private String policy;
}
