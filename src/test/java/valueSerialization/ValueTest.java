package valueSerialization;

import net.datafaker.Faker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Random;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

class ValueTest {
    private static final Random random = new Random();
    private static final Faker faker = new Faker(random);

    @ParameterizedTest
    @MethodSource("provideValueData")
    @DisplayName("test serialization and deserialization - sample data")
    void testSerializationAndDeserialization_UsingProvideData(Value input, String expectedJson) throws JsonProcessingException {
        var objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        String json = objectMapper.writeValueAsString(input);
        assertThatJson(json).isEqualTo(expectedJson);

        /*var deserialized = objectMapper.readValue(json, Value.class);

        var deserialized2 = objectMapper.readValue(expectedJson, Value.class);

        assertThat(deserialized)
                .usingRecursiveComparison()
                .isEqualTo(input);

        assertThat(deserialized)
                .usingRecursiveComparison()
                .isEqualTo(deserialized2);*/
    }

    private static Stream<Arguments> provideValueData() {
        return Stream.of(
                Arguments.of(null, "null"),

                Arguments.of(Blank.INSTANCE, """
                        {}"""),

                Arguments.of(new BooleanValue(false), """
                        {"boolean":false}"""),
                Arguments.of(new BooleanValue(true), """
                        {"boolean":true}"""),
                Arguments.of(new CurrencyValue(new BigDecimal("123456789012345678901234567890.1234567890123456789")), """
                        {"currency":123456789012345678901234567890.1234567890123456789}"""),
                Arguments.of(new DateValue(LocalDateTime.of(2025, 2, 1, 0, 0)), """
                        {"date":"2025-02-01"}"""),
                Arguments.of(new DateValue(LocalDateTime.of(2025, 2, 1, 23, 34, 56, 789)), """
                        {"date":"2025-02-01T23:34:56.000000789"}"""),
                Arguments.of(new DoubleValue(0), """
                        {"double":0.0}"""),
                Arguments.of(new DoubleValue(3.141592653589793), """
                        {"double":3.141592653589793}"""),
                Arguments.of(new DoubleValue(Double.MAX_VALUE), """
                        {"double":1.7976931348623157E+308}"""),
                Arguments.of(new ErrorValue(""), """
                        {"error":""}"""),
                Arguments.of(new ErrorValue("Something was wrong"), """
                        {"error":"Something was wrong"}"""),
                Arguments.of(new IntegerValue(0), """
                        {"integer":0}"""),
                Arguments.of(new IntegerValue(Integer.MAX_VALUE), """
                        {"integer":2147483647}"""),
                Arguments.of(new LongValue(1), """
                        {"long":1}"""),
                Arguments.of(new LongValue(Long.MAX_VALUE), """
                        {"long":9223372036854775807}"""),
                Arguments.of(new StringValue(""), """
                        {"string":""}"""),
                Arguments.of(new StringValue("Ala ma kota"), """
                        {"string":"Ala ma kota"}"""),
                Arguments.of(new TimeValue(LocalTime.of(12, 34)), """
                        {"time":"12:34"}"""),
                Arguments.of(new TimeValue(LocalTime.of(12, 34, 56)), """
                        {"time":"12:34:56"}"""),

                Arguments.of(new EnumValue<Color>(Color.BLUE), """
                        {"enum":"blue","type":"Color"}"""),
                Arguments.of(new EnumValue<Size>(Size.XL), """
                        {"enum":"xl","type":"Size"}"""),

                Arguments.of(new CustomValue<Price>(Price.of(3.14)), """
                        {"custom":"3.14","type":"Price"}"""),
                Arguments.of(new CustomValue<Price>(Price.mkt()), """
                        {"custom":"MKT","type":"Price"}""")
                );
    }

    @ParameterizedTest
    @MethodSource("faked")
    @DisplayName("test serialization and deserialization - generated")
    void testSerializationAndDeserialization_UsingFaker(Value input) throws JsonProcessingException {
        var objectMapper = new ObjectMapper();
        objectMapper.disable(SerializationFeature.INDENT_OUTPUT);
        String json = objectMapper.writeValueAsString(input);
        var deserialized = objectMapper.readValue(json, Value.class);

        System.out.println(json);

        assertThat(deserialized)
                .usingRecursiveComparison()
                .isEqualTo(input);
    }

    private static Stream<Value> faked() {
        return Stream.generate(ValueTest::getRandomValue)
                .limit(40);
    }

    private static Value getRandomValue() {
        int ruleIndex = random.nextInt(12) + 1;

        return switch (ruleIndex) {
            case 1 -> Blank.INSTANCE;
            case 2 -> new BooleanValue(random.nextBoolean());
            case 3 -> new CurrencyValue(BigDecimal.valueOf(random.nextDouble() * 100000000000.0));

            case 4 -> new DateValue()
                    ;
            case 5 ->
                    random.nextBoolean() ? new DoubleColumnDefinition(faker.lorem().word()) : new DoubleColumnDefinition();
            case 6 ->
                    random.nextBoolean() ? new StringColumnDefinition(faker.lorem().word()) : new StringColumnDefinition();

            case 7 -> new EnumColumnDefinition(getRandom(EnumColumnDefinition.ENUM_TYPE_TYPE_MAP.values()));
            case 8 -> new CustomColumnDefinition(getRandom(CustomColumnDefinition.CUSTOM_TYPE_TYPE_MAP.values()));

            case 9 -> CurrencyColumnDefinition.INSTANCE;
            case 10 -> CurrencyColumnDefinition.INSTANCE;
            case 11 -> CurrencyColumnDefinition.INSTANCE;
            case 12 -> CurrencyColumnDefinition.INSTANCE;
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



}