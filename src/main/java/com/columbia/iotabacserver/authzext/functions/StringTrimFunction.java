package com.columbia.iotabacserver.authzext.functions;

import com.columbia.iotabacserver.utils.LocalBeanFactory;
import org.ow2.authzforce.core.pdp.api.ImmutableXacmlStatus;
import org.ow2.authzforce.core.pdp.api.IndeterminateEvaluationException;
import org.ow2.authzforce.core.pdp.api.expression.Expression;
import org.ow2.authzforce.core.pdp.api.func.BaseFirstOrderFunctionCall;
import org.ow2.authzforce.core.pdp.api.func.FirstOrderFunctionCall;
import org.ow2.authzforce.core.pdp.api.func.SingleParameterTypedFirstOrderFunction;
import org.ow2.authzforce.core.pdp.api.value.BagDatatype;
import org.ow2.authzforce.core.pdp.api.value.Datatype;
import org.ow2.authzforce.core.pdp.api.value.StandardDatatypes;
import org.ow2.authzforce.core.pdp.api.value.StringValue;
import org.ow2.authzforce.xacml.identifiers.XacmlStatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Optional;


/**
 * An XACML extension function to trim string
 */
public final class StringTrimFunction extends SingleParameterTypedFirstOrderFunction<StringValue, StringValue> {
    private static final Logger logger = LoggerFactory.getLogger(StringTrimFunction.class);

    public static final String ID = "com:columbia:abac:xacml:3.0:function:trim";

    public StringTrimFunction() {
        super(ID, StandardDatatypes.STRING, false, Collections.singletonList(StandardDatatypes.STRING));
    }

    @Override
    public FirstOrderFunctionCall<StringValue> newCall(List<Expression<?>> argExpressions,
                                                       Datatype<?>... remainingArgTypes) throws IllegalArgumentException {
        return new BaseFirstOrderFunctionCall.EagerSinglePrimitiveTypeEval<>(functionSignature, argExpressions,
                remainingArgTypes) {

            @Override
            protected StringValue evaluate(Deque<StringValue> argStack) throws IndeterminateEvaluationException {
                if (argStack.isEmpty()) {
                    logger.error("Function {} can't take empty arguments.", ID);
                    throw new IndeterminateEvaluationException(new ImmutableXacmlStatus(XacmlStatusCode.SYNTAX_ERROR.value(),
                            Optional.of("Invalid function")));
                }
                return argStack.poll().trim();
            }
        };
    }
}
