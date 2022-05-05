package com.columbia.iotabacserver.dao.model;

import lombok.*;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Data
@ToString
public class DevRegisterPojo implements Serializable {
    private static final long serialVersionUID = -3818081785694362910L;

    String devId;
    String devType;
    String token;
    String attrs;
}
