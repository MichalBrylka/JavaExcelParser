package validation;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class Main {
    @lombok.SneakyThrows
    public static void main(String[] args) {


    }
}

class FieldRule {
    List<String> inputs;
    List<String> outputs;
    Rule mainRule;

    public String validate() {
        return (mainRule != null) ? mainRule.validate(inputs, outputs) : "No rule defined";
    }
}

sealed interface Rule permits Rule1, Rule2, Rule3 {
    default String validate(List<String> inputs, List<String> outputs) {
        String inputValidation = acceptedInputCardinality().validate(inputs.size());
        if (inputValidation != null) {
            return "Invalid inputs: " + inputValidation;
        }
        String outputValidation = acceptedOutputCardinality().validate(outputs.size());
        if (outputValidation != null) {
            return "Invalid outputs: " + outputValidation;
        }
        return additionalValidation();
    }

    Cardinality acceptedInputCardinality();

    Cardinality acceptedOutputCardinality();

    default String additionalValidation() {
        return null;
    }
}

record Rule1(String expression) implements Rule {
    @Override
    public Cardinality acceptedInputCardinality() {
        return Cardinality.one();
    }

    @Override
    public Cardinality acceptedOutputCardinality() {
        return Cardinality.oneOrMore();
    }

    @Override
    public String additionalValidation() {
        return (expression == null || expression.isEmpty()) ? "Expression cannot be empty" : null;
    }
}

record Rule2(Boolean append) implements Rule {
    @Override
    public Cardinality acceptedInputCardinality() {
        return Cardinality.oneOrMore();
    }

    @Override
    public Cardinality acceptedOutputCardinality() {
        return Cardinality.atMost(2);
    }

    @Override
    public String additionalValidation() {
        return (append == null) ? "Append flag must be specified" : null;
    }
}

record Rule3(Double price, Integer age) implements Rule {
    @Override
    public Cardinality acceptedInputCardinality() {
        return Cardinality.exactly(2);
    }

    @Override
    public Cardinality acceptedOutputCardinality() {
        return Cardinality.zero();
    }

    @Override
    public String additionalValidation() {
        return (price == null || age == null) ? "Price and age must be specified" : null;
    }
}

class Cardinality {
    private final int min;
    private final int max;

    private Cardinality(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public static Cardinality zero() {
        return new Cardinality(0, 0);
    }

    public static Cardinality one() {
        return new Cardinality(1, 1);
    }

    public static Cardinality exactly(int value) {
        return new Cardinality(value, value);
    }

    public static Cardinality atMost(int value) {
        return new Cardinality(Integer.MIN_VALUE, value);
    }

    public static Cardinality atLeast(int value) {
        return new Cardinality(value, Integer.MAX_VALUE);
    }

    public static Cardinality oneOrMore() {
        return new Cardinality(1, Integer.MAX_VALUE);
    }

    public String validate(int count) {
        if (count < min || count > max) {
            return "Expected cardinality between " + min + " and " + max + ", but got " + count;
        }
        return null;
    }
}