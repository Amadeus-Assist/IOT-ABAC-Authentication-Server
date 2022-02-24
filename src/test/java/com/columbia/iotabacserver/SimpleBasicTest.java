package com.columbia.iotabacserver;

import net.sf.saxon.s9api.*;
import org.junit.jupiter.api.Test;
import org.springframework.util.ResourceUtils;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;

public class SimpleBasicTest {

    @Test
    void simpleBasicTest(){
        List<String> list = Arrays.asList("abc","def");
        System.out.println(list);
    }

    @Test
    void simpleParserTest(){
        Processor processor = new Processor(false);
        DocumentBuilder builder = processor.newDocumentBuilder();
        String expression = "/Attributes/name/text()";
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
}
