package com.columbia.iotabacserver.dao.model;

import lombok.*;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Data
@ToString
public class ObjectHierarchyPojo implements Serializable {

    private static final long serialVersionUID = -2834137027231984102L;

    String objId;
    String hierarchy;
}
