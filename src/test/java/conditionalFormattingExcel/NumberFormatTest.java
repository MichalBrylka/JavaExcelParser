package conditionalFormattingExcel;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.of;

public class NumberFormatTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static Stream<Arguments> validSerializationCases() {
        return Stream.of(
                of(new NumberFormat(11, 22, 33), """
                        {
                          "#": 11,
                          "%": 22,
                          "$": 33
                        }
                        """),
                of(new NumberFormat(3, 1, 5), """
                        {
                          "#": 3,
                          "%": 1,
                          "$": 5
                        }
                        """)
        );
    }

    @ParameterizedTest(name = "✅ should serialize {0} correctly")
    @MethodSource("validSerializationCases")
    void shouldSerialize(NumberFormat input, String expectedJson) throws Exception {
        String actualJson = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(input);
        assertThat(actualJson).isEqualToIgnoringWhitespace(expectedJson);
    }

    static Stream<Arguments> validDeserializationCases() {
        return Stream.of(
                of("""
                        {}
                        """, new NumberFormat(null, null, null)),

                of("""
                        {
                          "#": 44
                        }
                        """, new NumberFormat(44, null, null)),

                of("""
                        {
                          "%": 11,
                          "$": 55
                        }
                        """, new NumberFormat(null, 11, 55)),

                of("""
                        {
                          "#": 3,
                          "%": 1,
                          "$": 0
                        }
                        """, new NumberFormat(3, 1, 0)),

                of("""
                        {
                          "#": null,
                          "%": null,
                          "$": null
                        }
                        """, new NumberFormat(null, null, null)),

                of("""
                        {  "%": 22, "$": 33 }
                        """, new NumberFormat(null, 22, 33)),
                of("""
                        { "#": 11, "$": 33 }
                        """, new NumberFormat(11, null, 33)),
                of("""
                        { "#": 11, "%": 22 }
                        """, new NumberFormat(11, 22, null))
        );
    }

    @ParameterizedTest(name = "✅ should deserialize to {1}")
    @MethodSource("validDeserializationCases")
    void shouldDeserialize(String json, NumberFormat expected) throws Exception {
        NumberFormat actual = MAPPER.readValue(json, NumberFormat.class);
        assertThat(actual).isEqualTo(expected);
    }

    static Stream<Arguments> invalidDeserializationCases() {
        return Stream.of(
                of("""
                        {
                          "#": 2,
                          "unknown": 7
                        }
                        """, "Unknown field: \"unknown\". Allowed fields are:"),

                of("""
                        {
                          "🚫": 9
                        }
                        """, "Unknown field: \"🚫\". Allowed fields are:")
        );
    }

    @ParameterizedTest(name = "❌ should fail deserializing invalid input: {1}")
    @MethodSource("invalidDeserializationCases")
    @DisplayName("❌ should throw exception on invalid JSON")
    void shouldFailDeserialization(String invalidJson, String expectedError) {
        assertThatThrownBy(() -> MAPPER.readValue(invalidJson, NumberFormat.class))
                .isInstanceOf(JsonMappingException.class)
                .hasMessageContaining(expectedError);
    }
}
