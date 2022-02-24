package com.columbia.iotabacserver.dao.model;

import lombok.*;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Data
@ToString
public class PolicyPojo implements Serializable {
    private static final long serialVersionUID = -1195235255303756748L;

    private String ref;
    private String version;
    private String type;
    private String content;
}
