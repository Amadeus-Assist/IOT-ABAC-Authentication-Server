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

    private static final long serialVersionUID = -2746844710915859221L;

    String ref;
    String content;
}
