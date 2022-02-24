package com.columbia.iotabacserver.dao.mapper;

import com.columbia.iotabacserver.dao.model.AttributesObjPojo;
import com.columbia.iotabacserver.dao.model.PolicyMapPojo;
import com.columbia.iotabacserver.dao.model.PolicyPojo;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AbacMapper {

    @Select("SELECT ref, type, version, content FROM xacml_policy_ref WHERE type=#{type} AND version=#{version} AND " +
            "ref=#{ref} LIMIT 1")
    PolicyPojo findRefPolicy(String ref, String type, String version);

    @Select("SELECT ref, type, version, content FROM xacml_policy_ref WHERE type=#{type} AND version REGEXP " +
            "#{versionRegex} AND " +
            "ref=#{ref} LIMIT 1")
    PolicyPojo findRefPolicyRegexVersion(String ref, String type, String version);

    @Select("SELECT ref, type, version, content FROM xacml_policy_ref WHERE type=#{type} AND ref=#{ref} LIMIT 1")
    PolicyPojo findRefPolicyWithoutVersion(String ref, String type);

    @Insert("INSERT INTO xacml_policy_ref(ref, type, version, content) VALUES(#{ref},#{type},#{version},#{content})")
    @Options(useGeneratedKeys = true)
    void insertPolicy(PolicyPojo pojo);

    @Update("UPDATE xacml_policy_ref SET content=#{pojo.content} WHERE ref=#{pojo.ref}")
    void updatePolicy(@Param("pojo") PolicyPojo pojo);

    @Insert("INSERT INTO columbia_members(ref, name, attributes) VALUES(#{ref},#{name},#{attributes})")
    @Options(useGeneratedKeys = true)
    void insertAttributes(AttributesObjPojo pojo);


    @Insert("INSERT INTO resource_act_policy_map(resource, action, policy_ref) VALUES(#{resource},#{action}," +
            "#{policy_ref})")
    @Options(useGeneratedKeys = true)
    void insertPolicyMap(PolicyMapPojo pojo);

    @Select("SELECT resource, action, policy_ref FROM resource_act_policy_map WHERE resource=#{resource} AND " +
            "action=#{action}")
    PolicyMapPojo findPolicyRef(String resource, String action);
}
