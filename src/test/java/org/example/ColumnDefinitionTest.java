package org.example;

import net.datafaker.Faker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collection;
import java.util.Random;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

class ColumnDefinitionTest {
    private static final Random random = new Random();
    private static final Faker faker = new Faker(random);

    @ParameterizedTest
    @MethodSource("provideColumnDefinitionData")
    @DisplayName("test serialization and deserialization - sample data")
    void testSerializationAndDeserialization_UsingProvideData(ColumnDefinition<?> input, String expectedJson) throws JsonProcessingException {
        var objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        String json = objectMapper.writeValueAsString(input);
        assertThatJson(json).isEqualTo(expectedJson);

        var deserialized = objectMapper.readValue(json, ColumnDefinition.class);

        var deserialized2 = objectMapper.readValue(expectedJson, ColumnDefinition.class);

        assertThat(deserialized)
                .usingRecursiveComparison()
                .isEqualTo(input);

        assertThat(deserialized)
                .usingRecursiveComparison()
                .isEqualTo(deserialized2);
    }

    private static Stream<Arguments> provideColumnDefinitionData() {
        return Stream.of(
                Arguments.of(null, "null"),

                Arguments.of(BooleanColumnDefinition.INSTANCE, """
                        {"kind":"boolean"}"""),
                Arguments.of(EmptyColumnDefinition.INSTANCE, """
                        {"kind":"empty"}"""),
                Arguments.of(IntegerColumnDefinition.INSTANCE, """
                        {"kind":"integer"}"""),
                Arguments.of(CurrencyColumnDefinition.INSTANCE, """
                        {"kind":"currency"}"""),

                Arguments.of(new DoubleColumnDefinition(), """
                        {"kind":"double","format":"#.##"}"""),
                Arguments.of(new DoubleColumnDefinition("#.##########"), """
                        {"kind":"double","format":"#.##########"}"""),
                Arguments.of(new DateColumnDefinition(), """
                        {"kind":"date","format":"yyyy/MM/dd"}"""),
                Arguments.of(new DateColumnDefinition("dd.MM.yyyy"), """
                        {"kind":"date","format":"dd.MM.yyyy"}"""),
                Arguments.of(new StringColumnDefinition(), """
                        {"kind":"string","format":""}"""),
                Arguments.of(new StringColumnDefinition("yyyy/MM/dd"), """
                        {"kind":"string","format":"yyyy/MM/dd"}"""),

                Arguments.of(new EnumColumnDefinition(Color.class), """
                        {"kind":"enum","type":"Color"}"""),
                Arguments.of(new EnumColumnDefinition(Size.class), """
                        {"kind":"enum","type":"Size"}"""),
                Arguments.of(new CustomColumnDefinition(Price.class), """
                        {"kind":"custom","type":"Price"}""")
        );
    }

    @ParameterizedTest
    @MethodSource("faked")
    @DisplayName("test serialization and deserialization - generated")
    void testSerializationAndDeserialization_UsingFaker(ColumnDefinition<?> input) throws JsonProcessingException {
        var objectMapper = new ObjectMapper();
        objectMapper.disable(SerializationFeature.INDENT_OUTPUT);
        String json = objectMapper.writeValueAsString(input);
        var deserialized = objectMapper.readValue(json, ColumnDefinition.class);

        System.out.println(json);

        assertThat(deserialized)
                .usingRecursiveComparison()
                .isEqualTo(input);
    }

    private static Stream<? extends ColumnDefinition<?>> faked() {
        return Stream.generate(ColumnDefinitionTest::getRandomColumnDefinition)
                .limit(40);
    }

    private static ColumnDefinition<?> getRandomColumnDefinition() {
        int ruleIndex = random.nextInt(9) + 1;

        return switch (ruleIndex) {
            case 1 -> BooleanColumnDefinition.INSTANCE;
            case 2 -> EmptyColumnDefinition.INSTANCE;
            case 3 -> IntegerColumnDefinition.INSTANCE;

            case 4 ->
                    random.nextBoolean() ? new DateColumnDefinition(faker.lorem().word()) : new DateColumnDefinition();
            case 5 ->
                    random.nextBoolean() ? new DoubleColumnDefinition(faker.lorem().word()) : new DoubleColumnDefinition();
            case 6 ->
                    random.nextBoolean() ? new StringColumnDefinition(faker.lorem().word()) : new StringColumnDefinition();

            case 7 -> new EnumColumnDefinition(getRandom(EnumColumnDefinition.ENUM_TYPE_TYPE_MAP.values()));
            case 8 -> new CustomColumnDefinition(getRandom(CustomColumnDefinition.CUSTOM_TYPE_TYPE_MAP.values()));

            case 9 -> CurrencyColumnDefinition.INSTANCE;
            default -> throw new IllegalStateException("Unexpected value: " + ruleIndex);
        };
    }

    public static <T> T getRandom(Collection<T> collection) {
        if (collection == null || collection.isEmpty())
            throw new IllegalArgumentException("Collection cannot be null or empty.");

        int randomIndex = random.nextInt(collection.size());

        var iterator = collection.iterator();
        T randomElement = null;
        for (int i = 0; i <= randomIndex; i++) {
            randomElement = iterator.next();
        }

        return randomElement;
    }



    @ParameterizedTest
    @MethodSource("provideCompactDeserializationData")
    @DisplayName("test deserialization of compact data")
    void testDeserialization_CompactData(ColumnDefinition<?> expected, String json) throws JsonProcessingException {
        var objectMapper = new ObjectMapper();

        var deserialized = objectMapper.readValue(json, ColumnDefinition.class);

        String json2 = objectMapper.writeValueAsString(deserialized);
        var deserialized2 = objectMapper.readValue(json2, ColumnDefinition.class);

        assertThat(deserialized)
                .usingRecursiveComparison()
                .isEqualTo(expected);

        assertThat(deserialized)
                .usingRecursiveComparison()
                .isEqualTo(deserialized2);
    }

    private static Stream<Arguments> provideCompactDeserializationData() {
        return Stream.of(
                Arguments.of(EmptyColumnDefinition.INSTANCE, "{}"),

                Arguments.of(new DoubleColumnDefinition(), """
                        {"kind":"double"}"""),
                Arguments.of(new DateColumnDefinition(), """
                        {"kind":"date"}"""),
                Arguments.of(new StringColumnDefinition(), """
                        {"kind":"string"}""")
        );
    }
}