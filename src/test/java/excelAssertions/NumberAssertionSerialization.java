package excelAssertions;

import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.assertj.core.data.Offset;
import org.assertj.core.data.Percentage;

// Enum with discriminator info + aliases (using varargs)
enum NumberAssertionType {
    EQUAL_TO("eq", "==", "="),
    GREATER_THAN("gt", ">"),
    GREATER_THAN_OR_EQUAL_TO("gte", ">="),
    LESS_THAN("lt", "<"),
    LESS_THAN_OR_EQUAL_TO("lte", "<="),
    CLOSE_TO_OFFSET("close", "~", "≈"),
    CLOSE_TO_PERCENT("closePercent", "close%", "≈%", "~%"),
    WITHIN_RANGE("in", "∈", "within"),
    OUTSIDE_RANGE("notIn", "∉", "out", "beyond");

    final String primary;
    final Set<String> aliases;

    NumberAssertionType(String primary, String... aliases) {
        Set<String> aliasSet = new HashSet<>();
        aliasSet.add(primary);
        Collections.addAll(aliasSet, aliases);
        this.aliases = Collections.unmodifiableSet(aliasSet);
        this.primary = primary;
    }

    static Optional<NumberAssertionType> fromDiscriminator(String disc) {
        return Arrays.stream(values())
                .filter(t -> t.aliases.contains(disc))
                .findFirst();
    }
}

class NumberAssertionSerializer extends JsonSerializer<NumberAssertion> {

    @Override
    public void serialize(NumberAssertion value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }

        gen.writeStartObject();

        switch (value) {
            case EqualToNumberAssertion eq -> gen.writeNumberField("eq", eq.expected());
            case GreaterThanNumberAssertion gt -> gen.writeNumberField("gt", gt.threshold());
            case GreaterThanOrEqualToNumberAssertion gte -> gen.writeNumberField("gte", gte.threshold());
            case LessThanNumberAssertion lt -> gen.writeNumberField("lt", lt.threshold());
            case LessThanOrEqualToNumberAssertion lte -> gen.writeNumberField("lte", lte.threshold());
            case CloseToOffsetNumberAssertion closeOffset -> {
                String formatted = String.format("%s±%s", closeOffset.expected(), closeOffset.offset().value);
                gen.writeStringField("close", formatted);
            }
            case CloseToPercentNumberAssertion closePercent -> {
                String formatted = String.format("%s±%s%%", closePercent.expected(), closePercent.percentage().value);
                gen.writeStringField("closePercent", formatted);
            }
            case WithinRangeNumberAssertion within -> {
                String text = String.valueOf(within.exclusiveFrom() ? '(' : '[') +
                              within.from() +
                              ".." +
                              within.to() +
                              (within.exclusiveTo() ? ')' : ']');
                gen.writeStringField("in", text);
            }
            case OutsideRangeNumberAssertion outside -> {
                String text = String.valueOf(outside.exclusiveFrom() ? '(' : '[') +
                              outside.from() +
                              ".." +
                              outside.to() +
                              (outside.exclusiveTo() ? ')' : ']');
                gen.writeStringField("notIn", text);
            }
            default -> throw new IllegalStateException("Unsupported NumberAssertion subclass: " + value.getClass());
        }

        gen.writeEndObject();
    }
}

class NumberAssertionDeserializer extends JsonDeserializer<NumberAssertion> {

    private static final Pattern CLOSE_TO_OFFSET_PATTERN = Pattern.compile("([-+]?\\d*\\.?\\d+)\\s*(±|\\+-)\\s*(\\d*\\.?\\d+)");
    private static final Pattern CLOSE_TO_PERCENT_PATTERN = Pattern.compile("([-+]?\\d*\\.?\\d+)\\s*(±|\\+-)\\s*(\\d*\\.?\\d+)%");
    private static final Pattern RANGE_PATTERN = Pattern.compile("([\\[(])\\s*([-+]?\\d*\\.?\\d+)\\s*\\.\\.\\s*([-+]?\\d*\\.?\\d+)\\s*([])])");

    @Override
    public NumberAssertion deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.getCurrentToken() != JsonToken.START_OBJECT) {
            throw JsonMappingException.from(p, "Expected start of object");
        }

        p.nextToken();
        if (p.getCurrentToken() != JsonToken.FIELD_NAME) {
            throw JsonMappingException.from(p, "Expected single field as discriminator");
        }

        String disc = p.currentName();
        NumberAssertionType type = NumberAssertionType.fromDiscriminator(disc)
                .orElseThrow(() -> JsonMappingException.from(p, "Unknown discriminator: " + disc));

        p.nextToken();

        NumberAssertion result;
        switch (type) {
            case EQUAL_TO -> result = new EqualToNumberAssertion(parseNumberOrThrow(p, ctxt));
            case GREATER_THAN -> result = new GreaterThanNumberAssertion(parseNumberOrThrow(p, ctxt));
            case GREATER_THAN_OR_EQUAL_TO ->
                    result = new GreaterThanOrEqualToNumberAssertion(parseNumberOrThrow(p, ctxt));
            case LESS_THAN -> result = new LessThanNumberAssertion(parseNumberOrThrow(p, ctxt));
            case LESS_THAN_OR_EQUAL_TO -> result = new LessThanOrEqualToNumberAssertion(parseNumberOrThrow(p, ctxt));
            case CLOSE_TO_OFFSET -> result = parseCloseToOffset(p, ctxt);
            case CLOSE_TO_PERCENT -> result = parseCloseToPercent(p, ctxt);
            case WITHIN_RANGE -> result = parseWithinRange(p, ctxt);
            case OUTSIDE_RANGE -> result = parseOutsideRange(p, ctxt);
            default -> throw JsonMappingException.from(p, "Unsupported discriminator: " + disc);
        }

        p.nextToken();
        if (p.getCurrentToken() != JsonToken.END_OBJECT) {
            throw JsonMappingException.from(p, "Expected end of object after single field");
        }

        return result;
    }

    private double parseNumberOrThrow(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken token = p.getCurrentToken();
        if (token == JsonToken.VALUE_NUMBER_FLOAT || token == JsonToken.VALUE_NUMBER_INT) {
            return p.getDoubleValue();
        }
        throw JsonMappingException.from(p, "Expected numeric value");
    }

    private CloseToOffsetNumberAssertion parseCloseToOffset(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.getCurrentToken() != JsonToken.VALUE_STRING) {
            throw JsonMappingException.from(p, "Expected string for close offset");
        }
        String s = p.getText().replace(" ", "");
        Matcher m = CLOSE_TO_OFFSET_PATTERN.matcher(s);
        if (!m.matches()) {
            throw JsonMappingException.from(p, "Invalid format for close offset: " + s);
        }
        double expected = Double.parseDouble(m.group(1));
        double offsetValue = Double.parseDouble(m.group(3));
        return new CloseToOffsetNumberAssertion(expected, Offset.offset(offsetValue));
    }

    private CloseToPercentNumberAssertion parseCloseToPercent(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.getCurrentToken() != JsonToken.VALUE_STRING) {
            throw JsonMappingException.from(p, "Expected string for close percent");
        }
        String s = p.getText().replace(" ", "");
        Matcher m = CLOSE_TO_PERCENT_PATTERN.matcher(s);
        if (!m.matches()) {
            throw JsonMappingException.from(p, "Invalid format for close percent: " + s);
        }
        double expected = Double.parseDouble(m.group(1));
        double percentValue = Double.parseDouble(m.group(3));
        return new CloseToPercentNumberAssertion(expected, Percentage.withPercentage(percentValue));
    }

    private WithinRangeNumberAssertion parseWithinRange(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.getCurrentToken() != JsonToken.VALUE_STRING) {
            throw JsonMappingException.from(p, "Expected string for within range");
        }
        String s = p.getText().replace(" ", "");
        Matcher m = RANGE_PATTERN.matcher(s);
        if (!m.matches()) {
            throw JsonMappingException.from(p, "Invalid format for within range: " + s);
        }
        boolean exclusiveFrom = m.group(1).equals("(");
        boolean exclusiveTo = m.group(4).equals(")");
        double from = Double.parseDouble(m.group(2));
        double to = Double.parseDouble(m.group(3));
        return new WithinRangeNumberAssertion(from, to, exclusiveFrom, exclusiveTo);
    }

    private OutsideRangeNumberAssertion parseOutsideRange(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.getCurrentToken() != JsonToken.VALUE_STRING) {
            throw JsonMappingException.from(p, "Expected string for outside range");
        }
        String s = p.getText().replace(" ", "");
        Matcher m = RANGE_PATTERN.matcher(s);
        if (!m.matches()) {
            throw JsonMappingException.from(p, "Invalid format for outside range: " + s);
        }
        boolean exclusiveFrom = m.group(1).equals("(");
        boolean exclusiveTo = m.group(4).equals(")");
        double from = Double.parseDouble(m.group(2));
        double to = Double.parseDouble(m.group(3));
        return new OutsideRangeNumberAssertion(from, to, exclusiveFrom, exclusiveTo);
    }
}
