package com.columbia.iotabacserver.dao.model;

import lombok.*;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Data
@ToString
public class DevCheckPojo implements Serializable {
    private static final long serialVersionUID = -2433868254961594296L;

    String devId;
    String devType;
    String token;
}
