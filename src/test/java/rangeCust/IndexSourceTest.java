package rangeCust;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static rangeCust.IndexSource.*;
import static rangeCust.IndexSource.all;

class IndexSourceTest {                              // 0    1    2    3    4
    private static final List<String> sample = List.of("a", "b", "c", "d", "e");
    private static final ObjectMapper mapper = new ObjectMapper();

    static Stream<Arguments> canResolveCases() {
        return Stream.of(
                Arguments.of(new SingleIndex(1), true),
                Arguments.of(new SingleIndex(-1), true),
                Arguments.of(new SingleIndex(5), false),
                Arguments.of(new Range(1, 3), true),
                Arguments.of(new Range(null, 2), true),
                Arguments.of(new Range(2, null), true),
                Arguments.of(new Range(4, 1), false),
                Arguments.of(new Range(0, 10), false),
                Arguments.of(new Range(null, null), true),
                Arguments.of(WholeRange.INSTANCE, true),
                Arguments.of(new CombiningRange(List.of(new SingleIndex(0), new Range(2, 3))), true),
                Arguments.of(new CombiningRange(List.of(new SingleIndex(0), new SingleIndex(10))), false)
        );
    }

    static Stream<Arguments> resolveCases() {
        return Stream.of(
                Arguments.of(single(0), List.of("a")),
                Arguments.of(single(-1), List.of("e")),
                Arguments.of(range(1, 3), List.of("b", "c", "d")),
                Arguments.of(range(null, 1), List.of("a", "b")),
                Arguments.of(range(2, null), List.of("c", "d", "e")),
                Arguments.of(range(null, null), sample),
                Arguments.of(all(), sample),
                Arguments.of(new CombiningRange(List.of(single(0), range(3, 4))), List.of("a", "d", "e")),
                Arguments.of(new CombiningRange(List.of(
                        single(1),
                        range(2, 4),
                        single(-1),
                        all()
                )), List.of(
                        "b",
                        "c", "d", "e",
                        "e",
                        "a", "b", "c", "d", "e"
                ))
        );
    }

    static Stream<Arguments> serializationCases() {
        return Stream.of(
                Arguments.of(single(2), "2"),
                Arguments.of(range(1, 4), "1..4"),
                Arguments.of(range(null, 3), "..3"),
                Arguments.of(range(2, null), "2.."),
                Arguments.of(all(), ".."),
                Arguments.of(new CombiningRange(List.of(single(0), range(1, 2), all())), "0,1..2,.."),
                Arguments.of(new CombiningRange(List.of(single(1), single(2))), "1,2"),
                Arguments.of(new CombiningRange(List.of(range(-2, -1), single(-5))), "-2..-1,-5")
        );
    }

    @ParameterizedTest(name = "✅ Can resolve: {0}")
    @MethodSource("canResolveCases")
    @DisplayName("canResolve")
    void testCanResolve(IndexSource source, boolean expected) {
        assertThat(source.canResolve(sample.size())).isEqualTo(expected);
    }

    @ParameterizedTest(name = "✅ Resolve: {0}")
    @MethodSource("resolveCases")
    @DisplayName("resolve")
    void testResolve(IndexSource source, List<String> expected) {
        assertThat(source.resolve(sample)).containsExactlyElementsOf(expected);
    }

    @ParameterizedTest(name = "✅ Serialize: {0}")
    @MethodSource("serializationCases")
    @DisplayName("serialize")
    void testSerialization(IndexSource source, String expectedJson) throws Exception {
        expectedJson = "\"%s\"".formatted(expectedJson);

        String json = mapper.writeValueAsString(source);
        assertThat(json).isEqualTo(expectedJson);
    }

    @ParameterizedTest(name = "✅ Deserialize: {1}")
    @MethodSource("serializationCases")
    @DisplayName("deserialize")
    void testDeserialization(IndexSource expected, String json) throws Exception {
        json = "\"%s\"".formatted(json);

        IndexSource parsed = mapper.readValue(json, IndexSource.class);
        assertThat(parsed).isEqualTo(expected);
    }

    @ParameterizedTest(name = "✅ Round-trip: {0}")
    @MethodSource("serializationCases")
    @DisplayName("round trip")
    void testRoundTrip(IndexSource original, String ignored) throws Exception {
        String json = mapper.writeValueAsString(original);
        IndexSource roundTrip = mapper.readValue(json, IndexSource.class);
        assertThat(roundTrip).isEqualTo(original);
    }


    static Stream<Arguments> compactingCases() {
        return Stream.of(
                Arguments.of(null, "null", null),
                Arguments.of(empty(), "\"\"", empty()),

                Arguments.of(new CombiningRange(List.of(
                                range(-2, -1),
                                empty(),
                                single(-8)
                        )), "\"-2..-1,-8\"",
                        new CombiningRange(List.of(
                                range(-2, -1),
                                single(-8)
                        ))
                ),
                Arguments.of(new CombiningRange(List.of()), "\"\"", empty()),

                Arguments.of(new CombiningRange(List.of(
                        new CombiningRange(List.of())
                )), "\"\"", empty()),

                Arguments.of(new CombiningRange(
                        List.of(
                                new CombiningRange(List.of()),
                                single(15)
                        )), "\"15\"", single(15))
        );
    }

    @ParameterizedTest(name = "✅ Compacting: {0}")
    @MethodSource("compactingCases")
    @DisplayName("compact")
    void compactingCases(IndexSource original, String expectedJson, IndexSource expected) throws Exception {
        String json = mapper.writeValueAsString(original);
        assertThat(json).as("serialize").isEqualTo(expectedJson);

        IndexSource deserialized = mapper.readValue(json, IndexSource.class);
        assertThat(deserialized).as("deserialize").isEqualTo(expected);
    }
}