package excelAssertions;

import org.assertj.core.data.Offset;
import org.assertj.core.data.Percentage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

class NumberAssertionTest {

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 1.0, 2.5, -3.0, 100.0, 0.001, Double.MAX_VALUE})
    void tt(double d) {
        var a = assertThat(d);


        a.isGreaterThanOrEqualTo(0.0);
        a.isLessThan(10.0);
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // --- Positive deserialization cases ---
    static Stream<Arguments> validDeserializationCases() {
        return Stream.of(
                Arguments.of("""
                        {"eq":5.5}
                        """, new EqualToNumberAssertion(5.5)),

                Arguments.of("""
                        {"==":10.0}
                        """, new EqualToNumberAssertion(10.0)),

                Arguments.of("""
                        {"gt":3.14}
                        """, new GreaterThanNumberAssertion(3.14)),

                Arguments.of("""
                        {"gte":7.0}
                        """, new GreaterThanOrEqualToNumberAssertion(7.0)),

                Arguments.of("""
                        {"lt":-1.0}
                        """, new LessThanNumberAssertion(-1.0)),

                Arguments.of("""
                        {"lte":0.0}
                        """, new LessThanOrEqualToNumberAssertion(0.0)),

                Arguments.of("""
                        {"close":100.0, "offset":5.0}
                        """, new CloseToOffsetNumberAssertion(100.0, Offset.offset(5.0))),

                Arguments.of("""
                        {"~":100.0, "offset":2.5}
                        """, new CloseToOffsetNumberAssertion(100.0, Offset.offset(2.5))),

                Arguments.of("""
                        {"close%":50.0, "percentage":"55%"}
                        """, new CloseToPercentNumberAssertion(50.0, Percentage.withPercentage(55.0))),

                Arguments.of("""
                        {"~%":50.0, "percentage":"20%"}
                        """, new CloseToPercentNumberAssertion(50.0, Percentage.withPercentage(20.0))),

                // New: percentage as number literal (0.55 meaning 55%)
                Arguments.of("""
                        {"close%":50.0, "percentage":0.55}
                        """, new CloseToPercentNumberAssertion(50.0, Percentage.withPercentage(55.0))),

                Arguments.of("""
                        {"~%":50.0, "percentage":0.2}
                        """, new CloseToPercentNumberAssertion(50.0, Percentage.withPercentage(20.0))),

                // discriminator not first, mixed order
                Arguments.of("""
                        {"offset":1.5, "close":10.0}
                        """, new CloseToOffsetNumberAssertion(10.0, Offset.offset(1.5))),

                Arguments.of("""
                        {"percentage":"25%", "close%":3.3}
                        """, new CloseToPercentNumberAssertion(3.3, Percentage.withPercentage(25.0)))
        );
    }

    @ParameterizedTest(name = "Deserialize valid JSON: {0}")
    @MethodSource("validDeserializationCases")
    @DisplayName("Deserialize valid JSON into NumberAssertion")
    void testValidDeserialization(String json, NumberAssertion expected) throws Exception {
        NumberAssertion actual = MAPPER.readValue(json, NumberAssertion.class);
        assertThat(actual).isEqualTo(expected);
    }

    // --- Negative deserialization cases ---
    static Stream<Named<String>> invalidDeserializationCases() {
        return Stream.of(
                Named.of("No discriminator field", "{}"),

                Named.of("Multiple discriminators",
                        """
                                {"eq":1, "gt":2}
                                """
                ),

                Named.of("Missing offset for close",
                        """
                                {"close":100}
                                """
                ),

                Named.of("Missing percentage for close%",
                        """
                                {"close%":50}
                                """
                ),

                Named.of("Percentage string without % sign",
                        """
                                {"close%":50, "percentage":"30"}
                                """
                ),

                Named.of("Percentage is boolean (invalid)",
                        """
                                {"close%":50, "percentage":true}
                                """
                ),

                Named.of("Percentage is object (invalid)",
                        """
                                {"close%":50, "percentage":{"val":55}}
                                """
                ),

                Named.of("Unknown field",
                        """
                                {"eq":5, "unknown":1}
                                """
                ),

                Named.of("Multiple discriminators with offset",
                        """
                                {"close":5, "gt":3, "offset":1}
                                """
                ),

                Named.of("Multiple discriminators with percentage",
                        """
                                {"close%":10, "eq":9, "percentage":"50%"}
                                """
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidDeserializationCases")
    @DisplayName("Deserialize invalid JSON throws JsonMappingException")
    void testInvalidDeserialization(String json) {
        Assertions.assertThatThrownBy(() -> MAPPER.readValue(json, NumberAssertion.class))
                .isInstanceOf(com.fasterxml.jackson.databind.JsonMappingException.class);
    }

    // --- Serialization tests ---
    static Stream<Arguments> serializationCases() {
        return Stream.of(
                Arguments.of(new EqualToNumberAssertion(5.5), """
                        {"eq":5.5}
                        """),
                Arguments.of(new GreaterThanNumberAssertion(3.14), """
                        {"gt":3.14}
                        """),
                Arguments.of(new CloseToOffsetNumberAssertion(100.0, Offset.offset(5.0)), """
                        {"close":100.0,"offset":5.0}
                        """),
                Arguments.of(new CloseToPercentNumberAssertion(50.0, Percentage.withPercentage(55.0)), """
                        {"close%":50.0,"percentage":"55.0%"}
                        """)
        );
    }

    @ParameterizedTest(name = "Serialize {0}")
    @MethodSource("serializationCases")
    @DisplayName("Serialize NumberAssertion to JSON")
    void testSerialization(NumberAssertion input, String expectedJson) throws Exception {
        String json = MAPPER.writeValueAsString(input);
        // Remove whitespace to compare cleanly
        String normalizedExpected = expectedJson.replaceAll("\\s", "");
        assertThat(json).isEqualTo(normalizedExpected);
    }
}
