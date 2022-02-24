package com.columbia.iotabacserver.utils;

import org.ow2.authzforce.xacml.identifiers.XacmlNodeName;
import org.ow2.authzforce.xacml.identifiers.XacmlVersion;

public class Constants {
    public static final String XACML3_POLICY_TYPE_ID =
            "{" + XacmlVersion.V3_0.getNamespace() + "}" + XacmlNodeName.POLICY.value();
    public static final String XACML3_POLICYSET_TYPE_ID =
            "{" + XacmlVersion.V3_0.getNamespace() + "}" + XacmlNodeName.POLICYSET.value();
    public static final String XML_NS_ABAC_COLUMBIA_EXT_1 = "http://columbia/abac/authentication/ext/1";
    public static final String XML_NS_ABAC_COLUMBIA_EXT_2 = "http://columbia/abac/authentication/ext/2";
}
