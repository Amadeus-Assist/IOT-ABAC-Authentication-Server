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


public class DBAuthInfoPojo {
    HashMap<String, String> dbAuthInfoMap;
    public DBAuthInfoPojo(String UnparsedDBAuthInfo) {
        String[] dbInfoPairs = UnparsedDBAuthInfo.split("[,:]");
        dbAuthInfoMap = new HashMap<String, String>();
        for(int i = 0; i < dbInfoPairs.length; i+=2) {
            dbAuthInfoMap.put(dbInfoPairs[i], dbInfoPairs[i+1]);
        }
    }
}
