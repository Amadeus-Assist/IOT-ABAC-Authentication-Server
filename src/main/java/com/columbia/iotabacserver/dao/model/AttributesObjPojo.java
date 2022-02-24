package com.columbia.iotabacserver.dao.model;

import lombok.*;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Data
@ToString
public class AttributesObjPojo implements Serializable {

    private static final long serialVersionUID = 4334326490001345370L;

    private String ref;
    private String name;
    private String attributes;
}
