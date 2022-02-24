package com.columbia.iotabacserver.utils;

import net.sf.saxon.s9api.*;
import org.ow2.authzforce.core.pdp.api.IndeterminateEvaluationException;
import org.ow2.authzforce.xacml.identifiers.XacmlStatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

public class XmlUtils {
    private static final Logger logger = LoggerFactory.getLogger(XmlUtils.class);

    public static XdmValue queryXmlNodeWithXpath(String xPath, XdmNode root, Processor processor) throws IndeterminateEvaluationException {
        if (!StringUtils.hasText(xPath) || root == null) {
            logger.error("Invalid arguments for queryXmlNodeWithXpath, xPath: {}, root: {}", xPath, root);
            throw new IllegalArgumentException("Invalid arguments for queryXmlNodeWithXpath");
        }
        String namespaceXPath = "//namespace::*";
        XdmValue targetNodes;
        try {
            XPathCompiler compiler = processor.newXPathCompiler();
            XdmValue namespaces = compiler.evaluate(namespaceXPath, root);
            for (XdmItem nsItem : namespaces) {
                XdmNode ns = (XdmNode) nsItem;
                QName prefix = ns.getNodeName();
                String uri = ns.getStringValue();
                compiler.declareNamespace(prefix == null ? "" : prefix.toString(), uri);
            }
            targetNodes = compiler.evaluate(xPath, root);
        } catch (SaxonApiException e) {
            logger.error("Fail to evaluate xPath, xpath: {}, msg: {}", xPath,
                    e.getStackTrace());
            throw new IndeterminateEvaluationException(XacmlStatusCode.PROCESSING_ERROR.value(), "Fail to evaluate " +
                    "xPath");
        }
        return targetNodes;
    }
}
