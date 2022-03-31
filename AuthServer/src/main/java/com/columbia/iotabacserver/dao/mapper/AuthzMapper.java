package com.columbia.iotabacserver.dao.mapper;

import com.columbia.iotabacserver.dao.model.ObjectHierarchyPojo;
import com.columbia.iotabacserver.dao.model.PolicyPojo;
import com.columbia.iotabacserver.dao.model.UserAttrsPojo;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AuthzMapper {

    @Select("SELECT ref, content FROM rego_policy_repository WHERE ref=#{ref} LIMIT 1")
    PolicyPojo findPolicy(String ref);

    @Insert("INSERT INTO rego_policy_repository(ref, content) VALUES(#{ref}, #{content})")
    @Options(useGeneratedKeys = true)
    void insertPolicy(PolicyPojo pojo);

    @Update("UPDATE rego_policy_repository SET content=#{pojo.content} WHERE ref=#{pojo.ref}")
    void updatePolicy(@Param("pojo") PolicyPojo pojo);

    @Select("SELECT obj_id AS objId, hierarchy FROM object_policy_hierarchy WHERE obj_id=#{objId} LIMIT 1")
    ObjectHierarchyPojo findHierarchy(String objId);

    @Insert("INSERT INTO object_policy_hierarchy(obj_id, hierarchy) VALUES(#{objId}, #{hierarchy})")
    @Options(useGeneratedKeys = true)
    void insertObjectHierarchy(ObjectHierarchyPojo pojo);

    @Select("SELECT user_id AS userId, attrs FROM user_attrs WHERE user_id=#{userId} LIMIT 1")
    UserAttrsPojo findUserAttrs(String userId);

    @Insert("INSERT INTO user_attrs(user_id, attrs) VALUES(#{userId}, #{attrs})")
    @Options(useGeneratedKeys = true)
    void insertUserAttrs(UserAttrsPojo pojo);

    @Update("UPDATE user_attrs SET attrs=#{pojo.attrs} WHERE user_id=#{pojo.userId}")
    void updateUserAttrs(@Param("pojo") UserAttrsPojo pojo);
}
