package com.columbia.iotabacserver.authzext.functions;

import com.columbia.iotabacserver.utils.LocalBeanFactory;
import com.columbia.iotabacserver.utils.XmlUtils;
import net.sf.saxon.s9api.*;
import org.ow2.authzforce.core.pdp.api.ImmutableXacmlStatus;
import org.ow2.authzforce.core.pdp.api.IndeterminateEvaluationException;
import org.ow2.authzforce.core.pdp.api.expression.Expression;
import org.ow2.authzforce.core.pdp.api.func.BaseFirstOrderFunctionCall;
import org.ow2.authzforce.core.pdp.api.func.FirstOrderFunctionCall;
import org.ow2.authzforce.core.pdp.api.func.MultiParameterTypedFirstOrderFunction;
import org.ow2.authzforce.core.pdp.api.func.SingleParameterTypedFirstOrderFunction;
import org.ow2.authzforce.core.pdp.api.value.*;
import org.ow2.authzforce.xacml.identifiers.XacmlStatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class AttributeSqlQueryFunction extends SingleParameterTypedFirstOrderFunction<Bag<StringValue>, StringValue> {
    private static final Logger logger = LoggerFactory.getLogger(AttributeSqlQueryFunction.class);

    public static final String ID = "com:columbia:abac:xacml:3.0:function:attr-sql-query";

    public AttributeSqlQueryFunction() {
        super(ID, StandardDatatypes.STRING.getBagDatatype(), true, Arrays.asList(StandardDatatypes.STRING,
                StandardDatatypes.STRING, StandardDatatypes.STRING));
    }

    @Override
    public FirstOrderFunctionCall<Bag<StringValue>> newCall(List<Expression<?>> argExpressions,
                                                            Datatype<?>... remainingArgTypes) throws IllegalArgumentException {
        return new BaseFirstOrderFunctionCall.EagerSinglePrimitiveTypeEval<>(functionSignature, argExpressions,
                remainingArgTypes) {
            @Override
            protected Bag<StringValue> evaluate(Deque<StringValue> argStack) throws IndeterminateEvaluationException {
                int idx = 0;
                String sqlTemp = null;
                String xpathExpression = null;
                List<String> sqlValues = new ArrayList<>();
                while (!argStack.isEmpty()) {
                    String arg = argStack.poll().toString();
                    if (idx == 0) {
                        sqlTemp = arg;
                    } else if (idx == 1) {
                        xpathExpression = arg;
                    } else {
                        sqlValues.add(arg);
                    }
                    idx++;
                }
                Connection conn = DataSourceUtils.getConnection((DataSource) LocalBeanFactory.getBean("dataSource"));
                String attributeObjContent;
                try {
                    PreparedStatement stmt = conn.prepareStatement(sqlTemp);
                    for (int i = 0; i < sqlValues.size(); i++) {
                        stmt.setString(i + 1, sqlValues.get(i));
                    }
                    ResultSet resultSet = stmt.executeQuery();
                    if (resultSet.next()) {
                        attributeObjContent = resultSet.getString(1);
                    } else {
                        logger.error("{} fail to query: empty result, sql template: {}, values: {}", ID, sqlTemp,
                                sqlValues);
                        throw new IndeterminateEvaluationException(XacmlStatusCode.PROCESSING_ERROR.value(),
                                "Function " + ID + ": get empty query result");
                    }

                } catch (SQLException throwables) {
                    logger.error("Fail to execute query for {}, error msg: {}", ID, throwables.getStackTrace());
                    throw new IndeterminateEvaluationException(XacmlStatusCode.SYNTAX_ERROR.value(), "Fail to " +
                            "execute quey for " + ID);
                }

//                logger.info("Retrieved attributes content: {}", attributeObjContent);

                assert xpathExpression != null;
                Bag<StringValue> targets;
                Processor processor = new Processor(false);
                DocumentBuilder builder = processor.newDocumentBuilder();
                try {
                    XdmNode root = builder.build(new StreamSource(new StringReader(attributeObjContent)));
                    if (xpathExpression.length() == 0) {
                        targets = Bags.singleton(StandardDatatypes.STRING, new StringValue(root.toString()));
                    } else {
                        XdmValue targetNodes = XmlUtils.queryXmlNodeWithXpath(xpathExpression, root, processor);
//                        logger.info("Target node size: {}", targetNodes.size());
                        Collection<StringValue> targetContents = new ArrayDeque<>();
                        for (XdmItem item : targetNodes) {
//                            logger.info("Target node content: {}", item.toString());
                            targetContents.add(new StringValue(item.toString()));
                        }
                        targets = Bags.newBag(StandardDatatypes.STRING, targetContents);
                    }
                } catch (SaxonApiException e) {
                    logger.error("Fail to build root node, attributeObjContent: {}, msg: {}", attributeObjContent,
                            e.getStackTrace());
                    throw new IndeterminateEvaluationException(XacmlStatusCode.PROCESSING_ERROR.value(), "Fail to " +
                            "build xml root node");
                }

                return targets;
            }
        };
    }

}

