package base64Enc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class EncodedObjectTest {

    static final ObjectMapper mapper = JsonMapper.builder()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .build();

    static Stream<Arguments> primitiveCases() {
        return Stream.of(
                Arguments.of(Named.of("✅ null", null), "null"),
                Arguments.of(Named.of("✅ null inner", new EncodedObject(null)), "null"),
                Arguments.of(Named.of("✅ string", new EncodedObject("hello")), """
                        "$base64:aGVsbG8=$\""""),
                Arguments.of(Named.of("✅ unicode", new EncodedObject("🐍")), """
                        "$base64:8J+QjQ==$\""""),
                Arguments.of(Named.of("✅ boolean true", new EncodedObject(true)), "true"),
                Arguments.of(Named.of("✅ boolean false", new EncodedObject(false)), "false"),
                Arguments.of(Named.of("✅ short", new EncodedObject((short) 7)), "7"),
                Arguments.of(Named.of("✅ int", new EncodedObject(32768)), "32768"),
                Arguments.of(Named.of("✅ long", new EncodedObject(1234567890123L)), "1234567890123"),
                Arguments.of(Named.of("✅ double", new EncodedObject(2.71828d)), "2.71828"),
                Arguments.of(Named.of("✅ BigInteger", new EncodedObject(new BigInteger("12345678901234567890"))), "12345678901234567890"),
                Arguments.of(Named.of("✅ BigDecimal", new EncodedObject(new BigDecimal("1.79769313486231570E+309"))), "1.79769313486231570E+309")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("primitiveCases")
    void shouldSerializeEncodedObject(EncodedObject input, String expectedJson) throws JsonProcessingException {
        String json = mapper.writeValueAsString(input);
        assertThat(json).isEqualTo(expectedJson);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("primitiveCases")
    void shouldDeserializeEncodedObject(EncodedObject expected, String json) throws JsonProcessingException {
        EncodedObject result = mapper.readValue(json, EncodedObject.class);

        if (expected == null || expected.value() == null)
            assertThat(result).isNull();
        else
            assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    record Box(Map<String, EncodedObject> payload) {
    }

    static Stream<Arguments> nestedCases() {
        return Stream.of(
                Arguments.of(Named.of("✅ nested string", new Box(Map.of("text", new EncodedObject("data")))), """
                        {"payload":{"text":"$base64:ZGF0YQ==$"}}"""),
                Arguments.of(Named.of("✅ nested mixed", new Box(
                        Stream.of(
                                Map.entry("num", new EncodedObject((short) 42)),
                                Map.entry("flag", new EncodedObject(true)),
                                Map.entry("msg", new EncodedObject("Hi 🌎"))
                        ).collect(
                                LinkedHashMap::new,
                                (m, e) -> m.put(e.getKey(), e.getValue()),
                                LinkedHashMap::putAll
                        )
                )), """
                        {"payload":{"num":42,"flag":true,"msg":"$base64:SGkg8J+Mjg==$"}}""")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nestedCases")
    void shouldSerializeNestedStructure(Box input, String expectedJson) throws JsonProcessingException {
        String json = mapper.writeValueAsString(input);
        assertThat(json).isEqualTo(expectedJson);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nestedCases")
    void shouldDeserializeNestedStructure(Box expected, String json) throws JsonProcessingException {
        Box result = mapper.readValue(json, Box.class);
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    static Stream<Arguments> valueTypeCases() {
        return Stream.of(
                Arguments.of(Named.of("null", new EncodedObject(null)), EncodedObject.ValueType.NULL),
                Arguments.of(Named.of("short", new EncodedObject((short) 1)), EncodedObject.ValueType.SHORT),
                Arguments.of(Named.of("int", new EncodedObject(1)), EncodedObject.ValueType.INTEGER),
                Arguments.of(Named.of("long", new EncodedObject(1L)), EncodedObject.ValueType.LONG),
                Arguments.of(Named.of("float", new EncodedObject(1.1f)), EncodedObject.ValueType.FLOAT),
                Arguments.of(Named.of("double", new EncodedObject(1.1d)), EncodedObject.ValueType.DOUBLE),
                Arguments.of(Named.of("BigInteger", new EncodedObject(BigInteger.TEN)), EncodedObject.ValueType.BIG_INTEGER),
                Arguments.of(Named.of("BigDecimal", new EncodedObject(BigDecimal.TEN)), EncodedObject.ValueType.BIG_DECIMAL),
                Arguments.of(Named.of("boolean", new EncodedObject(true)), EncodedObject.ValueType.BOOL),
                Arguments.of(Named.of("string", new EncodedObject("test")), EncodedObject.ValueType.STRING)
        );
    }

    @ParameterizedTest(name = "✅ type of {0}")
    @MethodSource("valueTypeCases")
    void shouldExposeCorrectValueType(EncodedObject obj, EncodedObject.ValueType expectedType) {
        assertThat(obj.getValueType()).isEqualTo(expectedType);
    }
}
