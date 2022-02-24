package com.columbia.iotabacserver.authzext.policyprovider;

import columbia.abac.authentication.ext._1.MySQLPolicyProviderDescriptor;
import com.columbia.iotabacserver.dao.mapper.AbacMapper;
import com.columbia.iotabacserver.dao.model.PolicyPojo;
import com.columbia.iotabacserver.utils.LocalBeanFactory;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.Policy;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.PolicySet;
import org.ow2.authzforce.core.pdp.api.EnvironmentProperties;
import org.ow2.authzforce.core.pdp.api.ImmutableXacmlStatus;
import org.ow2.authzforce.core.pdp.api.IndeterminateEvaluationException;
import org.ow2.authzforce.core.pdp.api.XmlUtils;
import org.ow2.authzforce.core.pdp.api.combining.CombiningAlgRegistry;
import org.ow2.authzforce.core.pdp.api.expression.ExpressionFactory;
import org.ow2.authzforce.core.pdp.api.policy.*;
import org.ow2.authzforce.core.pdp.impl.policy.PolicyEvaluators;
import org.ow2.authzforce.xacml.identifiers.XacmlNodeName;
import org.ow2.authzforce.xacml.identifiers.XacmlStatusCode;
import org.ow2.authzforce.xacml.identifiers.XacmlVersion;
import org.xml.sax.InputSource;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.StringReader;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;

public final class MySQLPolicyProvider extends BaseStaticPolicyProvider {
    /**
     * 'type' value expected in policy documents stored in database for XACML Policies
     */
    public static final String XACML3_POLICY_TYPE_ID =
            "{" + XacmlVersion.V3_0.getNamespace() + "}" + XacmlNodeName.POLICY.value();

    /**
     * 'type' value expected in policy documents stored in database for XACML PolicySets
     */
    public static final String XACML3_POLICYSET_TYPE_ID =
            "{" + XacmlVersion.V3_0.getNamespace() + "}" + XacmlNodeName.POLICYSET.value();


    /**
     * Factory
     */
    public static class Factory extends CloseablePolicyProvider.Factory<MySQLPolicyProviderDescriptor> {
        private static final IllegalArgumentException ILLEGAL_COMBINING_ALG_REGISTRY_ARGUMENT_EXCEPTION =
                new IllegalArgumentException("Undefined CombiningAlgorithm registry");
        private static final IllegalArgumentException ILLEGAL_EXPRESSION_FACTORY_ARGUMENT_EXCEPTION =
                new IllegalArgumentException("Undefined Expression factory");
        private static final IllegalArgumentException ILLEGAL_XACML_PARSER_FACTORY_ARGUMENT_EXCEPTION =
                new IllegalArgumentException("Undefined XACML parser factory");
        private static final IllegalArgumentException NULL_CONF_ARGUMENT_EXCEPTION = new IllegalArgumentException(
                "PolicyProvider configuration undefined");

        @Override
        public CloseablePolicyProvider<?> getInstance(MySQLPolicyProviderDescriptor conf,
                                                      XmlUtils.XmlnsFilteringParserFactory xmlParserFactory,
                                                      int maxPolicySetRefDepth, ExpressionFactory expressionFactory,
                                                      CombiningAlgRegistry combiningAlgRegistry,
                                                      EnvironmentProperties environmentProperties,
                                                      Optional<PolicyProvider<?>> otherHelpingPolicyProvider) throws IllegalArgumentException {
            if (conf == null) {
                throw NULL_CONF_ARGUMENT_EXCEPTION;
            }

            if (xmlParserFactory == null) {
                throw ILLEGAL_XACML_PARSER_FACTORY_ARGUMENT_EXCEPTION;
            }

            if (expressionFactory == null) {
                throw ILLEGAL_EXPRESSION_FACTORY_ARGUMENT_EXCEPTION;
            }

            if (combiningAlgRegistry == null) {
                throw ILLEGAL_COMBINING_ALG_REGISTRY_ARGUMENT_EXCEPTION;
            }

            return new MySQLPolicyProvider(conf.getId(), xmlParserFactory, expressionFactory, combiningAlgRegistry,
                    maxPolicySetRefDepth);
        }

        @Override
        public Class<MySQLPolicyProviderDescriptor> getJaxbClass() {
            return MySQLPolicyProviderDescriptor.class;
        }
    }

    private static final class PolicyQueryResult
    {
        private final PolicyPojo policyPojo;
        private final Object resultJaxbObj;
        private final Map<String, String> xmlnsToPrefixMap;


        private PolicyQueryResult(final PolicyPojo policyPojo, final Object resultJaxbObj, final Map<String, String> xmlnsToPrefixMap)
        {
            this.resultJaxbObj = resultJaxbObj;
            this.xmlnsToPrefixMap = xmlnsToPrefixMap;
            this.policyPojo = policyPojo;
        }
    }


    private final String id;
    private final XmlUtils.XmlnsFilteringParserFactory xacmlParserFactory;
    private final ExpressionFactory expressionFactory;
    private final CombiningAlgRegistry combiningAlgRegistry;
    private final AbacMapper abacMapper;
    private transient final ImmutableXacmlStatus jaxbUnmarshallerCreationErrStatus;


    /**
     * Creates RefPolicyProvider instance
     *
     * @param maxPolicySetRefDepth max policy reference (e.g. XACML PolicySetIdReference) depth, i.e. max length of
     *                             the chain of policy references
     */
    private MySQLPolicyProvider(final String id, final XmlUtils.XmlnsFilteringParserFactory xacmlParserFactory,
                                final ExpressionFactory expressionFactory,
                                final CombiningAlgRegistry combiningAlgRegistry, final int maxPolicySetRefDepth) {
        super(maxPolicySetRefDepth);
        assert id != null && !id.isEmpty() && xacmlParserFactory != null && expressionFactory != null && combiningAlgRegistry != null;

        this.id = id;
        this.xacmlParserFactory = xacmlParserFactory;
        this.expressionFactory = expressionFactory;
        this.combiningAlgRegistry = combiningAlgRegistry;
        this.abacMapper = LocalBeanFactory.getBean(AbacMapper.class);
        this.jaxbUnmarshallerCreationErrStatus = new ImmutableXacmlStatus(XacmlStatusCode.PROCESSING_ERROR.value(), Optional.of("PolicyProvider " + id + ": Failed to create JAXB unmarshaller for XACML Policy(Set)"));
    }

    private PolicyQueryResult getJaxbPolicyElement(final String policyTypeId, final String policyId, final Optional<PolicyVersionPatterns> policyPolicyVersionPatterns)
            throws IndeterminateEvaluationException
    {
        final Optional<PolicyVersionPattern> versionPattern;
        if (policyPolicyVersionPatterns.isPresent())
        {
            /*
             * TODO: the following code does not support LatestVersion and EarliestVersion patterns. Beware that comparing versions (XACML VersionType) to each other - and also comparing literal
             * version to version pattern (XACML VersionMatchType) - is NOT the same as sorting strings in lexicographical order or matching standard regular expressions. Indeed, in XACML, a version
             * (VersionType) is a sequence/array of decimal numbers actually, therefore it relies on number comparison; and version pattern use wildcard characters '*' and '+' with a special meaning
             * that is different from PCRE or other regex engines.
             */
            final PolicyVersionPatterns nonNullPolicyPolicyVersionPatterns = policyPolicyVersionPatterns.get();
            if (nonNullPolicyPolicyVersionPatterns.getEarliestVersionPattern().isPresent())
            {
                throw new IllegalArgumentException("PolicyProvider '" + id + "': EarliestVersion in input policy reference is not supported");
            }

            if (nonNullPolicyPolicyVersionPatterns.getLatestVersionPattern().isPresent())
            {
                throw new IllegalArgumentException("PolicyProvider '" + id + "': LatestVersion in input policy reference is not supported");
            }

            versionPattern = nonNullPolicyPolicyVersionPatterns.getVersionPattern();
        }
        else
        {
            versionPattern = Optional.empty();
        }

        final PolicyPojo policyPOJO;

        if (versionPattern.isPresent())
        {
            final PolicyVersionPattern nonNullVersionPattern = versionPattern.get();
            final PolicyVersion versionLiteral = nonNullVersionPattern.toLiteral();
            if (versionLiteral != null)
            {
                policyPOJO = abacMapper.findRefPolicy(policyId, policyTypeId, versionLiteral.toString());
            }
            else
            {
                /*
                 * versionPattern is not a literal/constant version (contains wildcard '*' or '+') -> convert to PCRE regex for MongoDB server-side evaluation
                 */
                final String regex = "^" + nonNullVersionPattern.toRegex() + "$";
                policyPOJO = abacMapper.findRefPolicyRegexVersion(policyId, policyTypeId, regex);
            }
        }
        else
        {
            // no version pattern specified
            policyPOJO = abacMapper.findRefPolicyWithoutVersion(policyId, policyTypeId);
        }

        if (policyPOJO == null)
        {
            return null;
        }

        final XmlUtils.XmlnsFilteringParser xacmlParser;
        try
        {
            xacmlParser = xacmlParserFactory.getInstance();
        }
        catch (final JAXBException e)
        {
            throw new IndeterminateEvaluationException(jaxbUnmarshallerCreationErrStatus, e);
        }

        final InputSource xmlInputSrc = new InputSource(new StringReader(policyPOJO.getContent()));
        final Object resultJaxbObj;
        try
        {
            /*
             * TODO: support more efficient formats of XML content, e.g. gzipped XML, Fast Infoset, EXI.
             */
            resultJaxbObj = xacmlParser.parse(xmlInputSrc);
        }
        catch (final JAXBException e)
        {
            throw new IndeterminateEvaluationException(
                    "PolicyProvider " + id + ": failed to parse Policy(Set) XML document from 'content' value of the policy document " + policyPOJO + " retrieved from database",
                    XacmlStatusCode.PROCESSING_ERROR.value(), e);
        }

        return new PolicyQueryResult(policyPOJO, resultJaxbObj, xacmlParser.getNamespacePrefixUriMap());
    }

    @Override
    protected StaticTopLevelPolicyElementEvaluator getPolicy(String policyIdRef,
                                                             Optional<PolicyVersionPatterns> constraints) throws IndeterminateEvaluationException {
        /*
         * TODO: use a policy cache and check it before requesting the database.
         */
        final PolicyQueryResult xmlParsingResult = getJaxbPolicyElement(XACML3_POLICY_TYPE_ID, policyIdRef, constraints);
        if (xmlParsingResult == null)
        {
            return null;
        }

        final PolicyPojo policyPOJO = xmlParsingResult.policyPojo;
        final Object jaxbPolicyOrPolicySetObj = xmlParsingResult.resultJaxbObj;
        final Map<String, String> nsPrefixUriMap = xmlParsingResult.xmlnsToPrefixMap;
        if (!(jaxbPolicyOrPolicySetObj instanceof Policy))
        {
            throw new IndeterminateEvaluationException("PolicyProvider " + id + ": 'content' of the policy document " + policyPOJO
                    + " retrieved from database is not consistent with its 'type' (expected: Policy). Actual content type: " + jaxbPolicyOrPolicySetObj.getClass() + " (corrupted database?).",
                    XacmlStatusCode.PROCESSING_ERROR.value());
        }

        final Policy jaxbPolicy = (Policy) jaxbPolicyOrPolicySetObj;
        final String contentPolicyId = jaxbPolicy.getPolicyId();
        if (!contentPolicyId.equals(policyPOJO.getRef()))
        {
            throw new IndeterminateEvaluationException("PolicyProvider " + id + ": PolicyId in 'content' of the policy document " + policyPOJO
                    + " retrieved from database is not consistent with 'id'. Actual PolicyId: " + contentPolicyId + " (corrupted database?).", XacmlStatusCode.PROCESSING_ERROR.value());
        }

        final String contentPolicyVersion = jaxbPolicy.getVersion();
        if (!contentPolicyVersion.equals(policyPOJO.getVersion()))
        {
            throw new IndeterminateEvaluationException("PolicyProvider " + id + ": Version in 'content' of the policy document " + policyPOJO
                    + " retrieved from database is not consistent with 'version'. Actual Version: " + contentPolicyVersion + " (corrupted database?).", XacmlStatusCode.PROCESSING_ERROR.value());
        }

        try
        {
            return PolicyEvaluators.getInstance(jaxbPolicy, null, nsPrefixUriMap, expressionFactory, combiningAlgRegistry);
        }
        catch (final IllegalArgumentException e)
        {
            throw new IllegalArgumentException("Invalid Policy in 'content' of the policy document " + policyPOJO + " retrieved from database", e);
        }
    }

    @Override
    protected StaticTopLevelPolicyElementEvaluator getPolicySet(String policyIdRef,
                                                                Optional<PolicyVersionPatterns> constraints,
                                                                Deque<String> policySetRefChainWithPolicyIdRef) throws IndeterminateEvaluationException {
        /*
         * TODO: use a policy cache and check it before requesting the database. If we found a matching policy in cache, and it is a policyset, we would check the depth of policy references as well:
         * <p>
         * Utils.appendAndCheckPolicyRefChain(newPolicySetRefChain, cachedPolicy.getExtraPolicyMetadata().getLongestPolicyRefChain(), maxPolicySetRefDepth);
         */
        final PolicyQueryResult xmlParsingResult = getJaxbPolicyElement(XACML3_POLICYSET_TYPE_ID, policyIdRef,
                constraints);
        if (xmlParsingResult == null)
        {
            return null;
        }

        final PolicyPojo policyPOJO = xmlParsingResult.policyPojo;
        final Object jaxbPolicyOrPolicySetObj = xmlParsingResult.resultJaxbObj;
        final Map<String, String> nsPrefixUriMap = xmlParsingResult.xmlnsToPrefixMap;
        if (!(jaxbPolicyOrPolicySetObj instanceof PolicySet))
        {
            throw new IndeterminateEvaluationException("PolicyProvider " + id + ": 'content' of the policy document " + policyPOJO
                    + " retrieved from database is not consistent with 'type' (expected: PolicySet). Actual content type: " + jaxbPolicyOrPolicySetObj.getClass() + " (corrupted database?).",
                    XacmlStatusCode.PROCESSING_ERROR.value());
        }

        final PolicySet jaxbPolicySet = (PolicySet) jaxbPolicyOrPolicySetObj;
        final String contentPolicyId = jaxbPolicySet.getPolicySetId();
        if (!contentPolicyId.equals(policyPOJO.getRef()))
        {
            throw new IndeterminateEvaluationException("PolicyProvider " + id + ": PolicyId in 'content' of the policy document " + policyPOJO
                    + " retrieved from database is not consistent with 'id'. Actual PolicyId: " + contentPolicyId + " (corrupted database?).", XacmlStatusCode.PROCESSING_ERROR.value());
        }

        final String contentPolicyVersion = jaxbPolicySet.getVersion();
        if (!contentPolicyVersion.equals(policyPOJO.getVersion()))
        {
            throw new IndeterminateEvaluationException("PolicyProvider " + id + ": Version in 'content' of the policy document " + policyPOJO
                    + " retrieved from database is not consistent with 'version'. Actual Version: " + contentPolicyVersion + " (corrupted database?).", XacmlStatusCode.PROCESSING_ERROR.value());
        }

        try
        {
            return PolicyEvaluators.getInstanceStatic(jaxbPolicySet, null, nsPrefixUriMap, expressionFactory, combiningAlgRegistry, this, policySetRefChainWithPolicyIdRef);
        }
        catch (final IllegalArgumentException e)
        {
            throw new IndeterminateEvaluationException("Invalid PolicySet in 'content' of the policy document " + policyPOJO + " retrieved from database", XacmlStatusCode.PROCESSING_ERROR.value(), e);
        }
    }

    @Override
    public void close() throws IOException {

    }
}
