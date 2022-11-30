package com.columbia.iotabacserver.dao.model;

import java.util.HashMap;
import lombok.*;
import java.time.LocalDate; 
import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Data
@ToString


public class DBAccessPermPojo implements Serializable {
    private static final long serialVersionUID = 398861713491327731L;
    String tableName;
    String userId;
    String allowDate;
    int allowLength;
    String denyDate;
    int denyLength;
}
