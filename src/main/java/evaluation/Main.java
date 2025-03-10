package evaluation;

import com.ezylang.evalex.EvaluationException;
import com.ezylang.evalex.Expression;
import com.ezylang.evalex.config.ExpressionConfiguration;
import com.ezylang.evalex.data.EvaluationValue;
import com.ezylang.evalex.functions.AbstractFunction;
import com.ezylang.evalex.functions.FunctionParameter;
import com.ezylang.evalex.parser.Token;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class Main {
    @lombok.SneakyThrows
    public static void main(String[] args) {
        var configuration = ExpressionConfiguration.builder()
                //.mathContext()
                .build()
                .withAdditionalFunctions(
                        Map.entry(IsNumberFunction.NAME, IsNumberFunction.INSTANCE)
                );
        Expression expression = new Expression("IF(IS_NUMBER(value), value < 5.0, NULL)", configuration)
                .with("value", 5.0)
                //.with("value", "CAT")
                ;

        EvaluationValue result = expression.evaluate();

        log.info("Result: {}", result.getBooleanValue());
    }

    @FunctionParameter(name = "value", isVarArg = true)
    static class IsNumberFunction extends AbstractFunction {
        public static final String NAME = "IS_NUMBER";

        public static final AbstractFunction INSTANCE = new IsNumberFunction();

        private IsNumberFunction() {
        }

        @Override
        public EvaluationValue evaluate(Expression expression, Token functionToken, EvaluationValue... parameterValues) throws EvaluationException {
            if (parameterValues.length != 1)
                throw new EvaluationException(functionToken, "%s requires exactly one parameter.".formatted(NAME));

            try {
                Double.parseDouble(parameterValues[0].getStringValue());
                return EvaluationValue.booleanValue(true);
            } catch (NumberFormatException e) {
                return EvaluationValue.booleanValue(false);
            }
        }
    }
}

