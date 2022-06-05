package com.columbia.iotabacserver.pojo.jackson_model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Setter
@Getter
@ToString
public class RuleJsonModel {
    private String key;
    private List<String> content;
}
