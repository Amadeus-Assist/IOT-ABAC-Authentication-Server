package com.columbia.iotabacserver.dao.model;

import lombok.*;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Data
@ToString
public class DevInfoPojo implements Serializable {
    private static final long serialVersionUID = -7454074520374920367L;

    String devId;
    String devType;
    String actions;
    String token;
    String attrs;
}
