package com.columbia.iotabacserver.dao.model;

import lombok.*;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Data
@ToString
public class DevActionsPojo implements Serializable {
    private static final long serialVersionUID = -4032626505120262005L;

    private String devId;
    private String actions;
}
