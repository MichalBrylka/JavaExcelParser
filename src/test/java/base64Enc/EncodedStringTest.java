package base64Enc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class EncodedStringTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    @ParameterizedTest(name = "{0}")
    @MethodSource("specialStringRoundTrip")
    void testSerialization(String json, EncodedString original) throws Exception {
        EncodedString deserialized = mapper.readValue(json, EncodedString.class);
        assertThat(deserialized)
                .usingRecursiveComparison()
                .isEqualTo(original);

        var json2 = mapper.writeValueAsString(deserialized);
        var deserialized2 = mapper.readValue(json2, EncodedString.class);

        assertThat(deserialized2)
                .usingRecursiveComparison()
                .isEqualTo(original);
    }

    static Stream<Arguments> specialStringRoundTrip() {
        return Stream.of(
                Arguments.of(Named.of("✅ Basic string", """
                        "Hello"
                        """), new EncodedString("Hello")),
                Arguments.of(Named.of("✅ BASE64-prefixed", """
                        "$base64:SGVsbG8=$"
                        """), new EncodedString("Hello")),
                Arguments.of(Named.of("✅ With emoji", """
                        "👨‍💻🚀"
                        """), new EncodedString("👨‍💻🚀")),

                Arguments.of(Named.of("✅ Empty", """
                        "$base64:$"
                        """), new EncodedString("")),
                Arguments.of(Named.of("✅ Null", "null"), null)
        );
    }


    @ParameterizedTest(name = "{0}")
    @MethodSource("nullSource")
    void testNull(Inner container) throws Exception {
        var json = mapper.writeValueAsString(container);

        var deserialized = mapper.readValue(json, Inner.class);

        assertThat(deserialized.payload)
                .isNull();
    }

    public static Stream<Arguments> nullSource() {
        return Stream.of(
                Arguments.of(Named.of("✅ NULL argument", new Inner(new EncodedString(null)))),
                Arguments.of(Named.of("✅ NULL", new Inner(null)))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("innerCases")
    void shouldSerializeInnerCorrectly(Inner input, String expectedJson) throws JsonProcessingException {
        String json = mapper.writeValueAsString(input);
        assertThat(json).isEqualTo(expectedJson);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("innerCases")
    void shouldDeserializeInnerCorrectly(Inner expected, String json) throws JsonProcessingException {
        Inner result = mapper.readValue(json, Inner.class);
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    static Stream<Arguments> innerCases() {
        return Stream.of(
                Arguments.of(Named.of("✅ Inner with payload", new Inner(new EncodedString("nested"))), """
                        {"payload":"$base64:bmVzdGVk$"}"""),
                Arguments.of(Named.of("✅ Inner with null payload", new Inner(null)), """
                        {"payload":null}""")
        );
    }

    private record Inner(EncodedString payload) {
    }


    static Stream<Arguments> encodingExamples() {
        return Stream.of(
                Arguments.of(Named.of("✅ Normal ASCII", "Hello"), "$base64:SGVsbG8=$"),
                Arguments.of(Named.of("✅ Unicode emoji", "🌍"), "$base64:8J+MjQ==$")
        );
    }

    static Stream<Arguments> decodingExamples() {
        return Stream.of(
                Arguments.of(Named.of("✅ Valid BASE64", "$base64:SGVsbG8=$"), "Hello"),
                Arguments.of(Named.of("✅ Raw text", "Some plain text"), "Some plain text")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("encodingExamples")
    void testEncoding(String input, String expected) {
        assertThat(EncodedString.encode(input))
                .isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("decodingExamples")
    void testDecoding(String input, String expected) {
        assertThat(EncodedString.decode(input))
                .isEqualTo(expected);
    }
}
