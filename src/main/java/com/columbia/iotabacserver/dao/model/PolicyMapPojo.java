package com.columbia.iotabacserver.dao.model;

import lombok.*;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Data
@ToString
public class PolicyMapPojo implements Serializable {

    private static final long serialVersionUID = -3609176875668487971L;

    private String resource;
    private String action;
    private String policy_ref;
}
