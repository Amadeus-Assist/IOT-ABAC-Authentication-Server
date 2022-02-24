package com.columbia.iotabacserver;


import columbia.abac.authentication.ext._2.Attributes;
import com.columbia.iotabacserver.dao.mapper.AbacMapper;
import com.columbia.iotabacserver.dao.model.AttributesObjPojo;
import com.columbia.iotabacserver.dao.model.PolicyMapPojo;
import com.columbia.iotabacserver.dao.model.PolicyPojo;
import com.columbia.iotabacserver.utils.Constants;
import com.columbia.iotabacserver.utils.LocalBeanFactory;
import com.columbia.iotabacserver.utils.XmlUtils;
import net.sf.saxon.s9api.*;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.*;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.ow2.authzforce.core.pdp.api.DecisionRequest;
import org.ow2.authzforce.core.pdp.api.DecisionRequestBuilder;
import org.ow2.authzforce.core.pdp.api.DecisionResult;
import org.ow2.authzforce.core.pdp.api.IndeterminateEvaluationException;
import org.ow2.authzforce.core.pdp.api.expression.AttributeSelectorExpression;
import org.ow2.authzforce.core.pdp.api.func.BaseFirstOrderFunctionCall;
import org.ow2.authzforce.core.pdp.api.func.EqualTypeMatchFunction;
import org.ow2.authzforce.core.pdp.api.io.PdpEngineInoutAdapter;
import org.ow2.authzforce.core.pdp.api.policy.PrimaryPolicyMetadata;
import org.ow2.authzforce.core.pdp.api.value.Bag;
import org.ow2.authzforce.core.pdp.api.value.XPathValue;
import org.ow2.authzforce.core.pdp.impl.BasePdpEngine;
import org.ow2.authzforce.core.pdp.impl.PdpEngineConfiguration;
import org.ow2.authzforce.core.pdp.impl.io.PdpEngineAdapters;
import org.ow2.authzforce.xacml.Xacml3JaxbHelper;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.util.ResourceUtils;

import javax.sql.DataSource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SpringBootTest
class IotabacserverApplicationTests {

    @Test
    void contextLoads() {
        AbacMapper mapper = LocalBeanFactory.getBean(AbacMapper.class);
        System.out.println("AbacMapper address is: " + mapper);
    }

    @Test
    void storePolicyToDatabase() {
        AbacMapper mapper = LocalBeanFactory.getBean(AbacMapper.class);
        String policyId = "samplepolicyset:1";
        String policyType = Constants.XACML3_POLICYSET_TYPE_ID;
        String version = "1.0";
        String contents = null;
        try {
            File file = ResourceUtils.getFile("classpath:samples\\policyset.xml");
            contents = Files.readString(file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        PolicyPojo pojo = new PolicyPojo(policyId, version, policyType, contents);
        mapper.insertPolicy(pojo);
    }

    @Test
    void policyUpdate() {
        AbacMapper mapper = LocalBeanFactory.getBean(AbacMapper.class);
        String policyId = "samplepolicy:1";
        String policyType = Constants.XACML3_POLICY_TYPE_ID;
        String version = "1.0";
        String contents = null;
        try {
            File file = ResourceUtils.getFile("classpath:samples\\policy1.xml");
            contents = Files.readString(file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        PolicyPojo pojo = new PolicyPojo(policyId, version, policyType, contents);
        mapper.updatePolicy(pojo);
    }

    @Test
    void localPolicyTest() {
        try {
            Unmarshaller unmarshaller = Xacml3JaxbHelper.createXacml3Unmarshaller();
            File file = ResourceUtils.getFile("classpath:samples\\policyset.xml");
            Object jaxbObj = unmarshaller.unmarshal(new FileReader(file));
            if (jaxbObj instanceof PolicySet) {
                PolicySet policySet = (PolicySet) jaxbObj;
                System.out.println("PolicySet version: " + policySet.getVersion());
            }
        } catch (JAXBException | FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Test
    void basicPolicyEvaluate() throws IOException {
//        XmlUtils.XmlnsFilteringParserFactory xacmlParserFactory = XacmlJaxbParsingUtils.getXacmlParserFactory(true);
//        int maxPolicySetRefDepth = -1;
//        CombiningAlgRegistry combiningAlgRegistry = StandardCombiningAlgorithm.REGISTRY;
//        AttributeValueFactoryRegistry attValFactoryRegistry = StandardAttributeValueFactories.getRegistry(true,
//        Optional.empty());

//        BasePdpEngine engine =
//                new BasePdpEngine(new MySQLPolicyProvider.Factory().getInstance(new MySQLPolicyProviderDescriptor()
//                , ))

        File pdpFile;
        File catalogFile;
        File extFile;
        PdpEngineConfiguration config = null;
        try {
            pdpFile = ResourceUtils.getFile("classpath:pdp.xml");
            catalogFile = ResourceUtils.getFile("classpath:authz\\catalog.cat");
            extFile = ResourceUtils.getFile("classpath:pdp-ext.xsd");
            config = PdpEngineConfiguration.getInstance(pdpFile, catalogFile.getAbsolutePath(),
                    extFile.getAbsolutePath());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (config == null) {
            throw new RuntimeException("No Configuration constructed.");
        }
        try {
            Field policyRootRef = config.getClass().getDeclaredField("rootPolicyId");
            policyRootRef.setAccessible(true);
            policyRootRef.set(config, "samplepolicyset:1");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        System.out.println("Changed root policy id: " + config.getRootPolicyId());
//        BasePdpEngine engine = new BasePdpEngine(config);
        PdpEngineInoutAdapter<Request, Response> engine = PdpEngineAdapters.newXacmlJaxbInoutAdapter(config);
//        for(PrimaryPolicyMetadata meta : engine.getApplicablePolicies()){
//            System.out.println(meta.getDescription().orElseGet(()->"Empty Description"));
//        }

        try {
            Unmarshaller unmarshaller = Xacml3JaxbHelper.createXacml3Unmarshaller();
            File file = ResourceUtils.getFile("classpath:samples\\samplerequest.xml");
            Object jaxbObj = unmarshaller.unmarshal(new FileReader(file));
            if (jaxbObj instanceof Request) {
                Request request = (Request) jaxbObj;
                Response response = engine.evaluate(request);
                if (response.getResults().get(0).getDecision() == DecisionType.PERMIT) {
                    System.out.println(DecisionType.PERMIT);
                } else {
                    System.out.println(DecisionType.DENY);
                }
                for (Result re : response.getResults()) {
                    System.out.println("Real Decision: " + re.getDecision());
                }
            }
        } catch (JAXBException | FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Test
    void sqlSessionTest() {
        Connection conn = DataSourceUtils.getConnection((DataSource) LocalBeanFactory.getBean("dataSource"));
//        SqlSessionFactory factory = (SqlSessionFactory) LocalBeanFactory.getBean("sqlSessionFactory");
//        SqlSessionTemplate sessionTemplate = new SqlSessionTemplate(factory);
        try {
//            PreparedStatement stmt = sessionTemplate.getConnection()
            PreparedStatement stmt = conn.prepareStatement("SELECT content FROM " +
                    "xacml_policy_ref WHERE ref=? AND id=? LIMIT 1");
//            PreparedStatement stmt = conn.prepareStatement("SELECT predicted_median_price AS content FROM " +
//                    "housing_median_prediction WHERE id=?");
            stmt.setString(1, "samplepolicy:1");
            stmt.setString(2, "1");
//            stmt.setString(1, "121");
//            stmt.setInt(1, 121);
            ResultSet res = stmt.executeQuery();

            res.next();
            System.out.println(res.getString("content"));

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    @Test
    void selfDefinedAttrTypeTest() {
        try {
//            Unmarshaller unmarshaller = Xacml3JaxbHelper.createXacml3Unmarshaller();
            JAXBContext context = JAXBContext.newInstance(Attributes.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            File file = ResourceUtils.getFile("classpath:samples\\sampleattributes.xml");
            Object jaxbObj = unmarshaller.unmarshal(new FileReader(file));
            if (jaxbObj instanceof Attributes) {
                Attributes obj = (Attributes) jaxbObj;
//                for (Attribute attr : obj.getAnies()) {
////                    System.out.println(attr);
//
//                    System.out.println("Attribute id: " + attr.getAttributeId() + ", value: " + attr
//                    .getAttributeValues());
//                }
            }
        } catch (JAXBException | FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Test
    void saxonParseTest() {
        Processor processor = new Processor(false);
        DocumentBuilder builder = processor.newDocumentBuilder();
//        Source xmlSource = new StreamSource();
        String expression = "/AttrObj";
        String retrieveNamespace = "//namespace::*";
        try {
            XdmNode root = builder.build(ResourceUtils.getFile("classpath:samples\\sampleattributes.xml"));
//            printXdmNode(root);
            XPathCompiler compiler = processor.newXPathCompiler();
//            compiler.declareNamespace("xacml", "urn:oasis:names:tc:xacml:3.0:core:schema:wd-17");
//            compiler.declareNamespace("", "http://columbia/abac/authentication/ext/2");
//            XPathExecutable exec = compiler.compile(retrieveNamespace);
//            XPathSelector selector = exec.load();
//            selector.setContextItem(root);
//            XdmValue result = selector.evaluate();
            XdmValue namespaces = compiler.evaluate(retrieveNamespace, root);
            for (XdmItem nsItem : namespaces) {
                XdmNode ns = (XdmNode) nsItem;
//                System.out.println("Prefix: " + (ns.getNodeName() == null ? "default" :
//                        ns.getNodeName().toString()) + ", uri: " + ns.getStringValue());
                QName prefix = ns.getNodeName();
                String uri = ns.getStringValue();
                compiler.declareNamespace(prefix == null ? "" : prefix.toString(), uri);
//                System.out.println(item);
            }
            XdmValue result = compiler.evaluate(expression, root);
            System.out.println("size of result: " + result.size());
            for (XdmItem item : result) {
                System.out.println(item.toString());
            }
        } catch (SaxonApiException | FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Test
    void insertColumbiaMembers() {
        AbacMapper mapper = LocalBeanFactory.getBean(AbacMapper.class);
        String ref = "bob145";
        String name = "bob";
        String attributes = null;
        try {
            File file = ResourceUtils.getFile("classpath:samples\\sampleattributes.xml");
            attributes = Files.readString(file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        AttributesObjPojo pojo = new AttributesObjPojo(ref, name, attributes);
        mapper.insertAttributes(pojo);
    }

    @Test
    void insertPolicyMap() {
        AbacMapper mapper = LocalBeanFactory.getBean(AbacMapper.class);
        String resource = "air_conditioner";
        String action = "open";
        String policy_ref = "samplepolicyset:1";
        PolicyMapPojo pojo = new PolicyMapPojo(resource, action, policy_ref);
        mapper.insertPolicyMap(pojo);
    }


    @Test
    void combineDatabasePolicyEvaluate() {
        AbacMapper mapper = LocalBeanFactory.getBean(AbacMapper.class);

        //Process request xml, get the target resource and actions, query database to retrieve the related
        //policy id
        Processor processor = new Processor(false);
        DocumentBuilder builder = processor.newDocumentBuilder();
        String resourceXpath = "//Attribute[@AttributeId='urn:oasis:names:tc:xacml:1" +
                ".0:resource:resource-type']/AttributeValue/text()";
        String actionXpath = "//Attribute[@AttributeId='urn:oasis:names:tc:xacml:1" +
                ".0:action:action-id']/AttributeValue/text()";

        List<String[]> resourceActionPolicyTuples = new ArrayList<>();

        try {
            XdmNode requestRoot = builder.build(ResourceUtils.getFile("classpath:samples\\samplerequest.xml"));
            XdmValue targetResources = XmlUtils.queryXmlNodeWithXpath(resourceXpath, requestRoot, processor);
            String resource = targetResources.iterator().next().getStringValue().trim();
            XdmValue targetActions = XmlUtils.queryXmlNodeWithXpath(actionXpath, requestRoot, processor);
            for (XdmItem actItem : targetActions) {
                String action = actItem.getStringValue().trim();
                PolicyMapPojo policyMapPojo = mapper.findPolicyRef(resource, action);
                String rootPolicyRef = policyMapPojo.getPolicy_ref();
                resourceActionPolicyTuples.add(new String[]{resource, action, rootPolicyRef});
            }
        } catch (SaxonApiException | FileNotFoundException | IndeterminateEvaluationException e) {
            e.printStackTrace();
        }

        //parse request to get root request object for later evaluation
        Request request = null;
        try {
            Unmarshaller unmarshaller = Xacml3JaxbHelper.createXacml3Unmarshaller();
            File file = ResourceUtils.getFile("classpath:samples\\samplerequest.xml");
            Object jaxbObj = unmarshaller.unmarshal(new FileReader(file));
            if (jaxbObj instanceof Request) {
                request = (Request) jaxbObj;
            } else {
                throw new RuntimeException("No request constructed.");
            }
        } catch (JAXBException | FileNotFoundException e) {
            e.printStackTrace();
        }


        //set up basic configuration for AuthzForce pdp engine
        File pdpFile;
        File catalogFile;
        File extFile;
        PdpEngineConfiguration config = null;
        try {
            pdpFile = ResourceUtils.getFile("classpath:pdp.xml");
            catalogFile = ResourceUtils.getFile("classpath:authz\\catalog.cat");
            extFile = ResourceUtils.getFile("classpath:pdp-ext.xsd");
            config = PdpEngineConfiguration.getInstance(pdpFile, catalogFile.getAbsolutePath(),
                    extFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (config == null) {
            throw new RuntimeException("No Configuration constructed.");
        }


        //set different root policy ref to configuration, to evaluate for different resource-action pairs
        try {
            for (String[] tuple : resourceActionPolicyTuples) {
                Field policyRootRef = config.getClass().getDeclaredField("rootPolicyId");
                policyRootRef.setAccessible(true);
                policyRootRef.set(config, tuple[2]);

                PdpEngineInoutAdapter<Request, Response> engine = PdpEngineAdapters.newXacmlJaxbInoutAdapter(config);
                Response response = engine.evaluate(request);
//                if (response.getResults().get(0).getDecision() == DecisionType.PERMIT) {
//                    System.out.println(DecisionType.PERMIT);
//                } else {
//                    System.out.println(DecisionType.DENY);
//                }
                for (Result re : response.getResults()) {
                    System.out.printf("resource: %s, action: %s, root policy ref: %s, decision: %s%n", tuple[0],
                            tuple[1], tuple[2], re.getDecision());
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException | IOException e) {
            e.printStackTrace();
        }
    }


    void printXdmNode(XdmNode node) {
        System.out.println(node);
        Iterable<XdmNode> iter = node.children();
        for (XdmNode child : iter) {
            printXdmNode(child);
        }
    }

}
