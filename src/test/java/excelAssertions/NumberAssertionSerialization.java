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
import java.util.function.Function;
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

    static Optional<NumberAssertionType> fromDiscriminator(String discriminator) {
        return Arrays.stream(values())
                .filter(t -> t.aliases.contains(discriminator))
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
    private static final String CLOSE_TO_PATTERN = "^(?<expected>[-+]?\\d*\\.?\\d+)\\s*(±|\\+-)\\s*(?<tolerance>\\d*\\.?\\d+)";
    private static final Pattern CLOSE_TO_OFFSET_PATTERN = Pattern.compile(CLOSE_TO_PATTERN + "$");
    private static final Pattern CLOSE_TO_PERCENT_PATTERN = Pattern.compile(CLOSE_TO_PATTERN + "%$");
    private static final Pattern RANGE_PATTERN = Pattern.compile(
            "^\\s*(?<open>[\\[(])\\s*" +
            "(?<from>[-+]?\\d*\\.?\\d+)\\s*\\.\\.\\s*" +
            "(?<to>[-+]?\\d*\\.?\\d+)\\s*" +
            "(?<close>[])])\\s*$"
    );

    @Override
    public NumberAssertion deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
        if (p.getCurrentToken() != JsonToken.START_OBJECT)
            throw JsonMappingException.from(p, "Expected start of object");

        p.nextToken();
        if (p.getCurrentToken() != JsonToken.FIELD_NAME)
            throw JsonMappingException.from(p, "Expected single field as discriminator");

        String disc = p.currentName();
        NumberAssertionType type = NumberAssertionType.fromDiscriminator(disc)
                .orElseThrow(() -> JsonMappingException.from(p, "Unknown discriminator: '%s'".formatted(disc)));

        p.nextToken();

        NumberAssertion result = switch (type) {
            case EQUAL_TO -> parseNumberAssertionOrThrow(p, EqualToNumberAssertion::new);
            case GREATER_THAN -> parseNumberAssertionOrThrow(p, GreaterThanNumberAssertion::new);
            case GREATER_THAN_OR_EQUAL_TO -> parseNumberAssertionOrThrow(p, GreaterThanOrEqualToNumberAssertion::new);
            case LESS_THAN -> parseNumberAssertionOrThrow(p, LessThanNumberAssertion::new);
            case LESS_THAN_OR_EQUAL_TO -> parseNumberAssertionOrThrow(p, LessThanOrEqualToNumberAssertion::new);
            case CLOSE_TO_OFFSET ->
                    parseCloseTo(p, CLOSE_TO_OFFSET_PATTERN, (expected, tolerance) -> new CloseToOffsetNumberAssertion(expected, Offset.offset(tolerance)));
            case CLOSE_TO_PERCENT ->
                    parseCloseTo(p, CLOSE_TO_PERCENT_PATTERN, (expected, tolerance) -> new CloseToPercentNumberAssertion(expected, Percentage.withPercentage(tolerance)));
            case WITHIN_RANGE -> parseRange(p, WithinRangeNumberAssertion::new);
            case OUTSIDE_RANGE -> parseRange(p, OutsideRangeNumberAssertion::new);
        };

        p.nextToken();
        if (p.getCurrentToken() != JsonToken.END_OBJECT)
            throw JsonMappingException.from(p, "Expected end of object after single field");

        return result;
    }

    private static <T extends NumberAssertion> T parseNumberAssertionOrThrow(JsonParser parser, Function<Double, T> assertionSupplier) throws IOException {
        return switch (parser.getCurrentToken()) {
            case VALUE_NUMBER_FLOAT, VALUE_NUMBER_INT -> assertionSupplier.apply(parser.getDoubleValue());
            case null, default -> throw JsonMappingException.from(parser, "Expected numeric value");
        };
    }

    private static <T extends NumberAssertion> T parseCloseTo(JsonParser parser, Pattern pattern, CloseToSupplier<T> closeToSupplier) throws IOException {
        if (parser.getCurrentToken() != JsonToken.VALUE_STRING)
            throw JsonMappingException.from(parser, "Expected string for close-to pattern");
        String text = parser.getText().replace(" ", "").trim();
        Matcher m = pattern.matcher(text);
        if (!m.matches())
            throw JsonMappingException.from(parser, "Invalid format for close-to pattern: " + text);

        double expected = Double.parseDouble(m.group("expected"));
        double tolerance = Double.parseDouble(m.group("tolerance"));
        return closeToSupplier.get(expected, tolerance);
    }

    private static <T extends NumberAssertion> T parseRange(JsonParser p, RangeSupplier<T> rangeSupplier) throws IOException {
        if (p.getCurrentToken() != JsonToken.VALUE_STRING)
            throw JsonMappingException.from(p, "Expected string for within/outside range");
        String s = p.getText().replace(" ", "");
        Matcher m = RANGE_PATTERN.matcher(s);
        if (!m.matches()) throw JsonMappingException.from(p, "Invalid format for within/outside range: " + s);
        boolean exclusiveFrom = m.group("open").equals("(");
        boolean exclusiveTo = m.group("close").equals(")");
        double from = Double.parseDouble(m.group("from"));
        double to = Double.parseDouble(m.group("to"));
        return rangeSupplier.get(from, to, exclusiveFrom, exclusiveTo);
    }

    @FunctionalInterface
    private interface CloseToSupplier<T extends NumberAssertion> {
        T get(double expected, double tolerance);
    }

    @FunctionalInterface
    private interface RangeSupplier<T extends NumberAssertion> {
        T get(double from, double to, boolean exclusiveFrom, boolean exclusiveTo);
    }
}
