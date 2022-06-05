package com.columbia.iotabacserver.pojo.jackson_model;

import lombok.*;

import java.util.Map;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class AccessRequestModel {
    private Map<?, ?> sub;
    private  Map<?, ?> obj;
    private String action;
    private  Map<?, ?> env;
}
