package excelAssertions;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.*;
import org.assertj.core.api.AbstractDoubleAssert;
import org.assertj.core.data.Offset;
import org.assertj.core.data.Percentage;

import java.io.IOException;
import java.util.*;

@com.fasterxml.jackson.databind.annotation.JsonSerialize(using = NumberAssertion.NumberAssertionSerializer.class)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = NumberAssertion.NumberAssertionDeserializer.class)
public sealed interface NumberAssertion
        permits EqualToNumberAssertion,
        GreaterThanNumberAssertion,
        GreaterThanOrEqualToNumberAssertion,
        LessThanNumberAssertion,
        LessThanOrEqualToNumberAssertion,
        CloseToOffsetNumberAssertion,
        CloseToPercentNumberAssertion {

    void apply(AbstractDoubleAssert<? extends AbstractDoubleAssert<?>> assertion);

    @Override
    String toString();

    enum NumberAssertionType {
        EQ("eq", "==", "="), GT("gt", ">"), GTE("gte", ">="), LT("lt", "<"), LTE("lte", "<="), CLOSE("close", "~"), CLOSE_PERCENT("close%", "~%");


        @lombok.Getter
        private final String canonical;
        private final String[] aliases;

        NumberAssertionType(String canonical, String... aliases) {
            this.canonical = canonical;
            this.aliases = aliases;
        }

        private static final Map<String, NumberAssertionType> lookup = new HashMap<>();

        static {
            for (NumberAssertionType type : values()) {
                lookup.put(type.canonical, type);
                for (String alias : type.aliases)
                    lookup.put(alias, type);
            }
        }

        public static NumberAssertionType fromKey(String key) {
            return lookup.get(key);
        }
    }

    final class NumberAssertionSerializer extends JsonSerializer<NumberAssertion> {
        @Override
        public void serialize(NumberAssertion value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartObject();

            switch (value) {
                case EqualToNumberAssertion eq ->
                        gen.writeNumberField(NumberAssertionType.EQ.getCanonical(), eq.expected());
                case GreaterThanNumberAssertion gt ->
                        gen.writeNumberField(NumberAssertionType.GT.getCanonical(), gt.threshold());
                case GreaterThanOrEqualToNumberAssertion gte ->
                        gen.writeNumberField(NumberAssertionType.GTE.getCanonical(), gte.threshold());
                case LessThanNumberAssertion lt ->
                        gen.writeNumberField(NumberAssertionType.LT.getCanonical(), lt.threshold());
                case LessThanOrEqualToNumberAssertion lte ->
                        gen.writeNumberField(NumberAssertionType.LTE.getCanonical(), lte.threshold());
                case CloseToOffsetNumberAssertion close -> {
                    gen.writeNumberField(NumberAssertionType.CLOSE.getCanonical(), close.expected());
                    gen.writeNumberField("offset", close.offset().value);
                }
                case CloseToPercentNumberAssertion close -> {
                    gen.writeNumberField(NumberAssertionType.CLOSE_PERCENT.getCanonical(), close.expected());
                    gen.writeStringField("percentage", close.percentage().value + "%");
                }
            }

            gen.writeEndObject();
        }
    }

    class NumberAssertionDeserializer extends JsonDeserializer<NumberAssertion> {


        @Override
        public NumberAssertion deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            NumberAssertionType foundType = null;
            Double operation = null;
            Double offset = null;
            String percentageStr = null;
            Double percentageNum = null;

            if (p.getCurrentToken() != JsonToken.START_OBJECT) {
                throw JsonMappingException.from(p, "Expected START_OBJECT");
            }

            p.nextToken(); // move to first field or END_OBJECT

            while (p.currentToken() == JsonToken.FIELD_NAME) {
                String fieldName = p.currentName();
                p.nextToken(); // move to field value

                NumberAssertionType type = NumberAssertionType.fromKey(fieldName);

                if (type != null) {
                    // Discriminator found
                    if (foundType != null) {
                        throw JsonMappingException.from(p, "Multiple discriminator keys found");
                    }
                    foundType = type;

                    if (!p.currentToken().isNumeric()) {
                        throw JsonMappingException.from(p, "Discriminator '" + fieldName + "' value must be a number");
                    }
                    operation = p.getDoubleValue();
                } else if ("offset".equals(fieldName)) {
                    if (!p.currentToken().isNumeric()) {
                        throw JsonMappingException.from(p, "'offset' field must be a number");
                    }
                    offset = p.getDoubleValue();
                } else if ("percentage".equals(fieldName)) {
                    // percentage can be numeric or string
                    if (p.currentToken().isNumeric()) {
                        percentageNum = p.getDoubleValue();
                    } else if (p.currentToken() == JsonToken.VALUE_STRING) {
                        percentageStr = p.getText();
                    } else {
                        throw JsonMappingException.from(p, "'percentage' field must be a number or percentage string");
                    }
                } else {
                    // Unknown field - either ignore or error, here we error:
                    throw JsonMappingException.from(p, "Unexpected field '" + fieldName + "'");
                }

                p.nextToken(); // advance to next field or END_OBJECT
            }

            if (foundType == null) {
                throw JsonMappingException.from(p, "No discriminator key found");
            }

            // Validate no extra discriminator keys present:
            // Not needed here as we catch multiple discriminator keys above already.

            // Build object according to discriminator
            return switch (foundType) {
                case EQ -> new EqualToNumberAssertion(operation);
                case GT -> new GreaterThanNumberAssertion(operation);
                case GTE -> new GreaterThanOrEqualToNumberAssertion(operation);
                case LT -> new LessThanNumberAssertion(operation);
                case LTE -> new LessThanOrEqualToNumberAssertion(operation);
                case CLOSE -> {
                    if (offset == null) {
                        throw JsonMappingException.from(p, "Missing required 'offset' for CloseToOffsetNumberAssertion");
                    }
                    yield new CloseToOffsetNumberAssertion(operation, Offset.offset(offset));
                }
                case CLOSE_PERCENT -> {
                    if (percentageStr == null && percentageNum == null) {
                        throw JsonMappingException.from(p, "Missing required 'percentage' for CloseToPercentNumberAssertion");
                    }
                    Percentage percentage;
                    if (percentageNum != null) {
                        // numeric fraction (0.55 means 55%)
                        percentage = Percentage.withPercentage(percentageNum * 100);
                    } else {
                        // parse percentage string (must end with %)
                        if (!percentageStr.endsWith("%")) {
                            throw JsonMappingException.from(p, "Percentage string must end with %");
                        }
                        try {
                            double val = Double.parseDouble(percentageStr.substring(0, percentageStr.length() - 1));
                            percentage = Percentage.withPercentage(val);
                        } catch (NumberFormatException e) {
                            throw JsonMappingException.from(p, "Invalid percentage format: " + percentageStr);
                        }
                    }
                    yield new CloseToPercentNumberAssertion(operation, percentage);
                }
            };
        }
    }
}

// == value
record EqualToNumberAssertion(double expected) implements NumberAssertion {
    @Override
    public void apply(AbstractDoubleAssert<? extends AbstractDoubleAssert<?>> assertion) {
        assertion.isEqualTo(expected);
    }

    @Override
    public String toString() {
        return "==" + expected;
    }
}

// > value
record GreaterThanNumberAssertion(double threshold) implements NumberAssertion {
    @Override
    public void apply(AbstractDoubleAssert<? extends AbstractDoubleAssert<?>> assertion) {
        assertion.isGreaterThan(threshold);
    }

    @Override
    public String toString() {
        return ">" + threshold;
    }
}

// >= value
record GreaterThanOrEqualToNumberAssertion(double threshold) implements NumberAssertion {
    @Override
    public void apply(AbstractDoubleAssert<? extends AbstractDoubleAssert<?>> assertion) {
        assertion.isGreaterThanOrEqualTo(threshold);
    }

    @Override
    public String toString() {
        return ">=" + threshold;
    }
}

// < value
record LessThanNumberAssertion(double threshold) implements NumberAssertion {
    @Override
    public void apply(AbstractDoubleAssert<? extends AbstractDoubleAssert<?>> assertion) {
        assertion.isLessThan(threshold);
    }

    @Override
    public String toString() {
        return "<" + threshold;
    }
}

// <= value
record LessThanOrEqualToNumberAssertion(double threshold) implements NumberAssertion {
    @Override
    public void apply(AbstractDoubleAssert<? extends AbstractDoubleAssert<?>> assertion) {
        assertion.isLessThanOrEqualTo(threshold);
    }

    @Override
    public String toString() {
        return "<=" + threshold;
    }
}

// ~ value ± offset
record CloseToOffsetNumberAssertion(double expected, Offset<Double> offset) implements NumberAssertion {
    static CloseToOffsetNumberAssertion of(double expected, double offsetValue) {
        return new CloseToOffsetNumberAssertion(expected, Offset.offset(offsetValue));
    }

    @Override
    public void apply(AbstractDoubleAssert<? extends AbstractDoubleAssert<?>> assertion) {
        assertion.isCloseTo(expected, offset);
    }

    @Override
    public String toString() {
        return "~" + expected + "±" + offset.value;
    }
}

// ~ value ± percent%
record CloseToPercentNumberAssertion(double expected, Percentage percentage) implements NumberAssertion {
    static CloseToPercentNumberAssertion of(double expected, double percentValue) {
        return new CloseToPercentNumberAssertion(expected, Percentage.withPercentage(percentValue));
    }

    @Override
    public void apply(AbstractDoubleAssert<? extends AbstractDoubleAssert<?>> assertion) {
        assertion.isCloseTo(expected, percentage);
    }

    @Override
    public String toString() {
        return "~" + expected + "±" + percentage.value + "%";
    }
}
