package org.example;

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
                        {" gff gfg ":"DEF\\nGHI"}""")
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
        return IntStream.range(0, 40).boxed().map( i-> new FieldTranslation(  faker.lorem().sentence(), faker.lorem().word() )  );
    }
}