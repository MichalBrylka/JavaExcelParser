package org.example;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import net.datafaker.Faker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

class FieldTranslationTest {
    @ParameterizedTest
    @MethodSource("provideFieldTranslationData")
    @DisplayName("test serialization and deserialization - sample data")
    void testSerializationAndDeserialization_UsingProvideData(FieldTranslation input, String expectedJson) throws JsonProcessingException {
        var objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        String json = objectMapper.writeValueAsString(input);
        assertThatJson(json).isEqualTo(expectedJson);

        var deserialized = objectMapper.readValue(json, FieldTranslation.class);

        var deserialized2 = objectMapper.readValue(expectedJson, FieldTranslation.class);

        assertThat(deserialized)
                .usingRecursiveComparison()
                .isEqualTo(input);

        assertThat(deserialized)
                .usingRecursiveComparison()
                .isEqualTo(deserialized2);
    }

    private static Stream<Arguments> provideFieldTranslationData() {
        return Stream.of(
                Arguments.of(null, "null"),

                Arguments.of(new FieldTranslation("ABC", "DEF"), """
                        {"ABC":"DEF"}"""),
                Arguments.of(new FieldTranslation("ABC", "Ala ma kota a kot ma Alę"), """
                        {"ABC":"Ala ma kota a kot ma Alę"}"""),
                Arguments.of(new FieldTranslation(" gff gfg ", "DEF\nGHI"), """
                        {" gff gfg ":"DEF\\nGHI"}"""),

                Arguments.of(new FieldTranslation("_ABC_", "_DEF_"), """
                        {"_ABC_":"_DEF_"}"""),
                Arguments.of(new FieldTranslation("field-with-hyphens", "translation-with-hyphens"), """
                        {"field-with-hyphens":"translation-with-hyphens"}"""),

                Arguments.of(new FieldTranslation("from1", "to1", new MyDateColumnDefinition("date format")), """
                        {"from1":"to1","columnDefinition":{"date":{"format":"date format"}}}"""),
                Arguments.of(new FieldTranslation("from-2", "to_2", new MyStringColumnDefinition("string format")), """
                        {"from-2":"to_2","columnDefinition":{"string":{"format":"string format"}}}""")
        );
    }


    @ParameterizedTest
    @MethodSource("faked")
    @DisplayName("test serialization and deserialization - generated")
    void testSerializationAndDeserialization_UsingFaker(FieldTranslation input) throws JsonProcessingException {
        var objectMapper = new ObjectMapper();
        objectMapper.disable(SerializationFeature.INDENT_OUTPUT);
        String json = objectMapper.writeValueAsString(input);
        var deserialized = objectMapper.readValue(json, FieldTranslation.class);

        System.out.println(json);

        assertThat(deserialized)
                .usingRecursiveComparison()
                .isEqualTo(input);
    }

    private static final Random random = new Random();
    private static final Faker faker = new Faker(random);

    private static Stream<FieldTranslation> faked() {
        return IntStream.range(0, 40).boxed().map(i -> new FieldTranslation(faker.lorem().sentence(), faker.lorem().word()));
    }


    @ParameterizedTest
    @MethodSource("invalidFieldTranslations")
    void testDeserialize_invalidInput(String json, String expectedErrorMessage) {
        var objectMapper = new ObjectMapper();

        assertThatExceptionOfType(JsonParseException.class)
                .isThrownBy(() -> objectMapper.readValue(json, FieldTranslation.class))
                .withMessageStartingWith(expectedErrorMessage);
    }

    static Stream<Arguments> invalidFieldTranslations() {
        return Stream.of(
                Arguments.of("""
                        {"oldField": "newField", "extraField": "extra"}
                        """, "Only one from-to field is supported i.e. { \"from\": \"to\" } in addition to optional column definition"),
                Arguments.of("""
                        {"oldField": "newField", "columnDefinition": {}, "extraField": "extra"}
                        """, "Invalid schema for FieldTranslation. Supported schema is {\"oldField\": \"translation\", \"columnDefinition\": {...optional column definition}}"),
                Arguments.of("""
                        {"oldField": 123}""", "Only text value nodes are supported"),
                Arguments.of("""
                        {"oldField": 123.15}""", "Only text value nodes are supported"),
                Arguments.of("""
                        {"oldField": true}""", "Only text value nodes are supported"),
                Arguments.of("""
                        {"oldField": {"complex": "object"}}""", "Only text value nodes are supported"),

                Arguments.of("""
                        {"oldField": ["value1", "value2"]}""", "Only text value nodes are supported"),

                Arguments.of("[]", """
                        Invalid schema for FieldTranslation. Supported schema is {"oldField": "translation", "columnDefinition": {...optional column definition}}"""),
                Arguments.of("{}", """
                        Only one from-to field is supported i.e. { "from": "to" } in addition to optional column definition"""),
                Arguments.of("""
                        "string"
                        """, """
                        Invalid schema for FieldTranslation. Supported schema is {"oldField": "translation", "columnDefinition": {...optional column definition}}""")
        );
    }
}