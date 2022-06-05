package com.columbia.iotabacserver.dao.model;

import lombok.*;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Data
@ToString
public class DevAttrsPojo implements Serializable {
    private static final long serialVersionUID = 7856821243261396725L;

    private String devId;
    private String attrs;
}
