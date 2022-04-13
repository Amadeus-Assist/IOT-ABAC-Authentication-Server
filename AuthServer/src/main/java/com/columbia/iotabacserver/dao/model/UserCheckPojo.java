package com.columbia.iotabacserver.dao.model;

import lombok.*;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Data
@ToString
public class UserCheckPojo implements Serializable {
    private static final long serialVersionUID = -3012902721130774253L;

    private String username;
    private String password;
}
