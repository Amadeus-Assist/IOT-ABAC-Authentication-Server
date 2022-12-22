package com.columbia.iotabacserver.dao.model;

import java.util.HashMap;
import lombok.*;
import java.time.LocalDate; 
import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonProperty;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Data
@ToString


public class DBAccessPermPojo implements Serializable {
    private static final long serialVersionUID = 398861713491327731L;
    @JsonProperty("table_name")
    String tableName;
    @JsonProperty("user_id")
    String userId;
    @JsonProperty("db_access_date")
    String allowDate;
    @JsonProperty("db_allow_length")
    int allowLength;
    @JsonProperty("db_deny_date")
    String denyDate;
    @JsonProperty("db_deny_length")
    int denyLength;
}
