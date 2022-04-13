package com.columbia.iotabacserver.dao.mapper;

import com.columbia.iotabacserver.dao.model.*;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AuthzMapper {

    @Select("SELECT ref, content FROM rego_policy_repository WHERE ref=#{ref} LIMIT 1")
    PolicyPojo findPolicy(String ref);

    @Insert("INSERT INTO rego_policy_repository(ref, content) VALUES(#{ref}, #{content})")
    @Options(useGeneratedKeys = true)
    void insertPolicy(PolicyPojo pojo);

    @Update("UPDATE rego_policy_repository SET content=#{com.columbia.iotabacserver.pojo.content} WHERE ref=#{com.columbia.iotabacserver.pojo.ref}")
    void updatePolicy(@Param("com/columbia/iotabacserver/pojo") PolicyPojo pojo);

    @Select("SELECT obj_id AS objId, action, hierarchy FROM object_action_policy_hierarchy WHERE obj_id=#{objId} " +
            "AND action=#{action} LIMIT 1")
    ObjectHierarchyPojo findHierarchy(String objId, String action);

    @Insert("INSERT INTO object_action_policy_hierarchy(obj_id, action, hierarchy) VALUES(#{objId}, #{action}, #{hierarchy})")
    @Options(useGeneratedKeys = true)
    void insertObjectHierarchy(ObjectHierarchyPojo pojo);

    @Update("UPDATE object_action_policy_hierarchy SET hierarchy=#{com.columbia.iotabacserver.pojo.hierarchy} WHERE obj_id=#{com.columbia.iotabacserver.pojo.objId} AND " +
            "action=#{com.columbia.iotabacserver.pojo.action}")
    void updateObjectHierarchy(ObjectHierarchyPojo pojo);

    @Select("SELECT user_id AS userId, attrs FROM user_attrs WHERE user_id=#{userId} LIMIT 1")
    UserAttrsPojo findUserAttrs(String userId);

    @Insert("INSERT INTO user_attrs(user_id, attrs) VALUES(#{userId}, #{attrs})")
    @Options(useGeneratedKeys = true)
    void insertUserAttrs(UserAttrsPojo pojo);

    @Update("UPDATE user_attrs SET attrs=#{com.columbia.iotabacserver.pojo.attrs} WHERE user_id=#{com.columbia.iotabacserver.pojo.userId}")
    void updateUserAttrs(@Param("com/columbia/iotabacserver/pojo") UserAttrsPojo pojo);

    @Select("SELECT user_id AS username, pwd as password FROM user_attrs WHERE user_id=#{userId} LIMIT 1")
    UserCheckPojo findUserCheckInfo(String userId);

    @Select("SELECT dev_id AS devId, dev_type AS devType, token FROM dev_access_tokens WHERE dev_id=#{devId} LIMIT 1")
    DevCheckPojo findDevCheckInfo(String devId);

    @Insert("INSERT INTO dev_access_tokens(dev_id, dev_type, token) VALUES(#{devId}, #{devType}, #{token})")
    @Options(useGeneratedKeys = true)
    void insertDevCheckInfo(DevCheckPojo pojo);
}
