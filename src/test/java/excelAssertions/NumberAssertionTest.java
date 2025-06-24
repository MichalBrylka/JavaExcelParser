package excelAssertions;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.AbstractDoubleAssert;
import org.assertj.core.data.Offset;
import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class NumberAssertionTest {

    @ParameterizedTest(name = "{1} {0}")
    @MethodSource("passingCases")
    void apply_Positive(NumberAssertion assertion, double actual) {
        AbstractDoubleAssert<?> base = assertThat(actual);
        assertion.apply(base); // should not throw
    }

    @ParameterizedTest(name = "NOT {1} {0}")
    @MethodSource("failingCases")
    void apply_Negative(NumberAssertion assertion, double actual) {
        assertThatThrownBy(() -> assertion.apply(assertThat(actual)))
                .isInstanceOf(AssertionError.class);
    }

    static Stream<Arguments> passingCases() {
        return Stream.of(
                arguments(new EqualToNumberAssertion(5.5), 5.5), // == 5.5
                arguments(new GreaterThanNumberAssertion(3.0), 3.1), // > 3.0
                arguments(new GreaterThanOrEqualToNumberAssertion(2.0), 2.0), // ≥ 2.0
                arguments(new LessThanNumberAssertion(10.0), 9.9), // < 10.0
                arguments(new LessThanOrEqualToNumberAssertion(10.0), 10.0), // ≤ 10.0
                arguments(new CloseToOffsetNumberAssertion(5.0, Offset.offset(0.5)), 5.4), // ~5.0±0.5
                arguments(new CloseToPercentNumberAssertion(100.0, Percentage.withPercentage(5)), 104.0), // ~100.0±5%
                arguments(new WithinRangeNumberAssertion(1, 5), 1.0), // ∈ [1..5]
                arguments(new WithinRangeNumberAssertion(1, 5, true, true), 3.0), // ∈ (1..5)
                arguments(new OutsideRangeNumberAssertion(1, 5), 0.9), // ∉ [1..5]
                arguments(new OutsideRangeNumberAssertion(1, 5, true, true), 1.0), // ∉ (1..5)
                arguments(new OutsideRangeNumberAssertion(1, 5, true, true), 5.0), // ∉ (1..5)

                // Edge cases - passing
                arguments(new EqualToNumberAssertion(Double.POSITIVE_INFINITY), Double.POSITIVE_INFINITY), // == ∞
                arguments(new EqualToNumberAssertion(Double.NEGATIVE_INFINITY), Double.NEGATIVE_INFINITY), // == -∞
                arguments(new LessThanNumberAssertion(Double.POSITIVE_INFINITY), 1e308), // < ∞
                arguments(new GreaterThanNumberAssertion(Double.NEGATIVE_INFINITY), -1e308), // > -∞
                arguments(new WithinRangeNumberAssertion(Double.MIN_VALUE, Double.MIN_VALUE * 2), Double.MIN_VALUE), // ∈ [min..2min]
                arguments(new WithinRangeNumberAssertion(Double.MAX_VALUE - 1e292, Double.MAX_VALUE), Double.MAX_VALUE) // ∈ [MAX-1e292..MAX]
        );
    }

    static Stream<Arguments> failingCases() {
        return Stream.of(
                arguments(new EqualToNumberAssertion(5.5), 5.4), // == 5.5
                arguments(new GreaterThanNumberAssertion(3.0), 2.9), // > 3.0
                arguments(new GreaterThanOrEqualToNumberAssertion(2.0), 1.9), // ≥ 2.0
                arguments(new LessThanNumberAssertion(10.0), 10.1), // < 10.0
                arguments(new LessThanOrEqualToNumberAssertion(10.0), 10.1), // ≤ 10.0
                arguments(new CloseToOffsetNumberAssertion(5.0, Offset.offset(0.5)), 5.6), // ~5.0±0.5
                arguments(new CloseToPercentNumberAssertion(100.0, Percentage.withPercentage(5)), 106.0), // ~100.0±5%
                arguments(new WithinRangeNumberAssertion(1, 5), 0.99), // ∈ [1..5]
                arguments(new WithinRangeNumberAssertion(1, 5, true, true), 1.0), // ∈ (1..5)
                arguments(new OutsideRangeNumberAssertion(1, 5), 3.0), // ∉ [1..5]
                arguments(new OutsideRangeNumberAssertion(1, 5, true, true), 3.0), // ∉ (1..5)

                // Edge cases - failing
                arguments(new EqualToNumberAssertion(Double.NaN), Double.NaN), // == NaN
                arguments(new EqualToNumberAssertion(Double.NaN), 0.0), // == NaN
                arguments(new EqualToNumberAssertion(Double.POSITIVE_INFINITY), 1e308), // == ∞
                arguments(new EqualToNumberAssertion(Double.NEGATIVE_INFINITY), -1e308), // == -∞
                arguments(new LessThanNumberAssertion(Double.POSITIVE_INFINITY), Double.POSITIVE_INFINITY), // < ∞
                arguments(new GreaterThanNumberAssertion(Double.NEGATIVE_INFINITY), Double.NEGATIVE_INFINITY), // > -∞
                arguments(new WithinRangeNumberAssertion(Double.MIN_VALUE, Double.MIN_VALUE * 2), 0.0), // ∈ [min..2min]
                arguments(new WithinRangeNumberAssertion(Double.MAX_VALUE - 1e292, Double.MAX_VALUE), 0.0) // ∈ [MAX-1e292..MAX]
        );
    }


    private static final ObjectMapper MAPPER = new ObjectMapper();

    @ParameterizedTest
    @MethodSource("serializationPositiveFlow")
    void serialization_Positive(NumberAssertion input, String expectedJson) throws Exception {
        String json = MAPPER.writeValueAsString(input);
        assertThat(json).isEqualToIgnoringWhitespace(expectedJson);
    }

    @ParameterizedTest
    @MethodSource("serializationPositiveFlow")
    void deserialization_Positive(NumberAssertion expected, String json) throws Exception {
        NumberAssertion actual = MAPPER.readValue(json, NumberAssertion.class);
        assertThat(actual).isEqualTo(expected);
    }

    static Stream<Arguments> serializationPositiveFlow() {
        return Stream.of(
                arguments(new EqualToNumberAssertion(42.0), """
                        {"eq":42.0}"""),
                arguments(new GreaterThanNumberAssertion(10.0), """
                        {"gt":10.0}"""),
                arguments(new GreaterThanOrEqualToNumberAssertion(5.5), """
                        {"gte":5.5}"""),
                arguments(new LessThanNumberAssertion(3.14), """
                        {"lt":3.14}"""),
                arguments(new LessThanOrEqualToNumberAssertion(2.71), """
                        {"lte":2.71}"""),
                arguments(new CloseToOffsetNumberAssertion(100.0, Offset.offset(0.5)), """
                        {"close":"100.0±0.5"}"""),
                arguments(new CloseToPercentNumberAssertion(50.0, Percentage.withPercentage(10.0)), """
                        {"closePercent":"50.0±10.0%"}"""),
                arguments(new WithinRangeNumberAssertion(1.0, 10.0, false, true), """
                        {"in":"[1.0..10.0)"}"""),
                arguments(new OutsideRangeNumberAssertion(0.0, 100.0, true, false), """
                        {"notIn":"(0.0..100.0]"}""")
        );
    }

    @ParameterizedTest
    @MethodSource("deserialization_PositiveAlternative")
    void deserialization_PositiveAlternative(NumberAssertion expected, String json) throws Exception {
        NumberAssertion actual = MAPPER.readValue(json, NumberAssertion.class);
        assertThat(actual).isEqualTo(expected);
    }

    static Stream<Arguments> deserialization_PositiveAlternative() {
        return Stream.of(
                // EqualToNumberAssertion aliases
                arguments(new EqualToNumberAssertion(42.0), """
                        {"eq":42.0}"""),
                arguments(new EqualToNumberAssertion(42.0), """
                        {"==":42.0}"""),
                arguments(new EqualToNumberAssertion(42.0), """
                        {"=":42.0}"""),

                // GreaterThanNumberAssertion aliases
                arguments(new GreaterThanNumberAssertion(10.0), """
                        {"gt":10.0}"""),
                arguments(new GreaterThanNumberAssertion(10.0), """
                        {">":10.0}"""),

                // GreaterThanOrEqualToNumberAssertion aliases
                arguments(new GreaterThanOrEqualToNumberAssertion(5.5), """
                        {"gte":5.5}"""),
                arguments(new GreaterThanOrEqualToNumberAssertion(5.5), """
                        {">=":5.5}"""),

                // LessThanNumberAssertion aliases
                arguments(new LessThanNumberAssertion(3.14), """
                        {"lt":3.14}"""),
                arguments(new LessThanNumberAssertion(3.14), """
                        {"<":3.14}"""),

                // LessThanOrEqualToNumberAssertion aliases
                arguments(new LessThanOrEqualToNumberAssertion(2.71), """
                        {"lte":2.71}"""),
                arguments(new LessThanOrEqualToNumberAssertion(2.71), """
                        {"<=":2.71}"""),

                // CloseToOffsetNumberAssertion with ± and +-
                arguments(new CloseToOffsetNumberAssertion(100.0, Offset.offset(0.5)), """
                        {"close":"100.0±0.5"}"""),
                arguments(new CloseToOffsetNumberAssertion(100.0, Offset.offset(0.5)), """
                        {"close":"100.0+-0.5"}"""),
                arguments(new CloseToOffsetNumberAssertion(100.0, Offset.offset(0.5)), """
                        {"~":"100.0±0.5"}"""),
                arguments(new CloseToOffsetNumberAssertion(100.0, Offset.offset(0.5)), """
                        {"≈":"100.0+-0.5"}"""),

                // CloseToPercentNumberAssertion with ± and +-
                arguments(new CloseToPercentNumberAssertion(50.0, Percentage.withPercentage(10.0)), """
                        {"closePercent":"50.0±10.0%"}"""),
                arguments(new CloseToPercentNumberAssertion(50.0, Percentage.withPercentage(10.0)), """
                        {"close%":"50.0+-10.0%"}"""),
                arguments(new CloseToPercentNumberAssertion(50.0, Percentage.withPercentage(10.0)), """
                        {"≈%":"50.0±10.0%"}"""),
                arguments(new CloseToPercentNumberAssertion(50.0, Percentage.withPercentage(10.0)), """
                        {"~%":"50.0+-10.0%"}"""),

                // WithinRangeNumberAssertion aliases with brackets and parentheses
                arguments(new WithinRangeNumberAssertion(1.0, 10.0, false, true), """
                        {"in":"[1.0..10.0)"}"""),
                arguments(new WithinRangeNumberAssertion(1.0, 10.0, false, true), """
                        {"∈":"[1.0..10.0)"}"""),
                arguments(new WithinRangeNumberAssertion(1.0, 10.0, false, true), """
                        {"within":"[1.0..10.0)"}"""),

                // OutsideRangeNumberAssertion aliases with brackets and parentheses
                arguments(new OutsideRangeNumberAssertion(0.0, 100.0, true, false), """
                        {"notIn":"(0.0..100.0]"}"""),
                arguments(new OutsideRangeNumberAssertion(0.0, 100.0, true, false), """
                        {"∉":"(0.0..100.0]"}"""),
                arguments(new OutsideRangeNumberAssertion(0.0, 100.0, true, false), """
                        {"out":"(0.0..100.0]"}"""),
                arguments(new OutsideRangeNumberAssertion(0.0, 100.0, true, false), """
                        {"beyond":"(0.0..100.0]"}""")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("serializationNegativeFlow")
    void deserialization_Negative(String json, String expectedErrorMessagePart) {
        assertThatThrownBy(() -> MAPPER.readValue(json, NumberAssertion.class))
                .isInstanceOf(JsonMappingException.class)
                .hasMessageContaining(expectedErrorMessagePart);
    }

    static Stream<Arguments> serializationNegativeFlow() {
        return Stream.of(
                arguments(Named.of("Empty JSON object", "{}"), "Expected single field as discriminator"),
                arguments(Named.of("Unknown discriminator field", """
                        {"unknown":123}"""), "Unknown discriminator: 'unknown'"),
                arguments(Named.of("Multiple fields", """
                        {"eq":42, "gt":10}"""), "Expected end of object after single field"),
                arguments(Named.of("Wrong value type for number discriminator", """
                        {"eq":"not_a_number"}"""), "Expected numeric value"),
                arguments(Named.of("Invalid close offset format", """
                        {"close":"100+0.5"}"""), "Invalid format for close-to pattern: 100+0.5"),
                arguments(Named.of("Invalid close percent format", """
                        {"closePercent":"50±abc%"}"""), "Invalid format for close-to pattern: 50±abc%"),
                arguments(Named.of("Invalid within range format", """
                        {"in":"[1..ten]"}"""), "Invalid format for within/outside range: [1..ten]"),
                arguments(Named.of("Invalid outside range format", """
                        {"notIn":"(zero..100)"}"""), "Invalid format for within/outside range: (zero..100)"),
                arguments(Named.of("Not an object", """
                        "eq\""""), "Expected start of object"),
                arguments(Named.of("Empty field name", """
                        {"":42}"""), "Unknown discriminator: ''")
        );
    }
}
