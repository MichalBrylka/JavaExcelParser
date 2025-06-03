package conditionalFormattingExcel;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

class FormatTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @ParameterizedTest
    @MethodSource("provideFormatsForSerialization")
    void shouldSerializeAndDeserializeFormat(Format originalFormat, String expectedJson) throws Exception {
        // Serialize
        String actualJson = objectMapper.writeValueAsString(originalFormat);
        assertThat(actualJson).isEqualTo(expectedJson);

        // Deserialize
        Format deserialized = objectMapper.readValue(expectedJson, Format.class);
        assertThat(deserialized).isEqualTo(originalFormat);
        assertThat(deserialized.getPoiFormatString()).isEqualTo(originalFormat.getPoiFormatString());
    }

    static Stream<Arguments> provideFormatsForSerialization() {
        return Stream.of(
                Arguments.of(new Format.PercentFormat(2), """
                        {"percent":2}"""),
                Arguments.of(new Format.PercentFormat(0), """
                        {"percent":0}"""),
                Arguments.of(new Format.FixedFormat(3), """
                        {"fixed":3}"""),
                Arguments.of(new Format.CurrencyFormat(2, "$"), """
                        {"currency":"2;$"}"""),
                Arguments.of(new Format.CurrencyFormat(0, "€"), """
                        {"currency":"0;€"}""")
        );
    }

    static Stream<Arguments> invalidJsonCases() {
        return Stream.of(
                Arguments.of("❌ More than one field",
                        """
                                {
                                  "percent": 2,
                                  "fixed": 1
                                }
                                """,
                        "Format object must contain at most one field: 'percent', 'fixed', or 'currency'"
                ),

                Arguments.of("❌ Unknown field key",
                        """
                                {
                                  "unknown": 3
                                }
                                """,
                        "Unknown format key: unknown"
                ),

                Arguments.of("❌ Percent not an integer",
                        """
                                {
                                  "percent": "two"
                                }
                                """,
                        "'percent' must be an integer"
                ),

                Arguments.of("❌ Fixed not an integer",
                        """
                                {
                                  "fixed": true
                                }
                                """,
                        "'fixed' must be an integer"
                ),

                Arguments.of("❌ Currency is not a string",
                        """
                                {
                                  "currency": 5
                                }
                                """,
                        "'currency' must be a non blank string in format 'decimalPlaces;symbol'"
                ),

                Arguments.of("❌ Currency missing symbol part",
                        """
                                {
                                  "currency": "5"
                                }
                                """,
                        "expected 'decimalPlaces;symbol'"
                ),

                Arguments.of("❌ Currency decimal part is not a number",
                        """
                                {
                                  "currency": "abc;$"
                                }
                                """,
                        "Invalid decimalPlaces in currency format"
                ),

                Arguments.of("❌ Currency symbol is empty",
                        """
                                {
                                  "currency": "3;"
                                }
                                """,
                        "currencySymbol must not be null or blank"
                ),

                Arguments.of("❌ Currency symbol is whitespace",
                        """
                                {
                                  "currency": "3;   "
                                }
                                """,
                        "currencySymbol must not be null or blank"
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidJsonCases")
    void shouldThrowJsonMappingExceptionForInvalidInputs(String description, String json, String expectedMessage) {
        assertThatThrownBy(() -> objectMapper.readValue(json, Format.class))
                .isInstanceOf(JsonMappingException.class)
                .hasMessageContaining(expectedMessage);
    }


    @Test
    void shouldDeserializeEmptyObjectToDefaultFixedFormat() throws Exception {
        Format format = objectMapper.readValue("{}", Format.class);

        assertThat(format).isInstanceOf(Format.FixedFormat.class);
        assertThat(((Format.FixedFormat) format).decimalPlaces()).isEqualTo(2);
    }
}