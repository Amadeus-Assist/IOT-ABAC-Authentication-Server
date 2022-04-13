package com.columbia.iotabacserver.dao.model;

import lombok.*;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Data
@ToString
public class UserAttrsPojo implements Serializable {
    private static final long serialVersionUID = -7586961713491327731L;

    String userId;
    String password;
    String attrs;
}
