package valueSerialization;

import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.io.IOException;

import java.util.stream.Collectors;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;

import static valueSerialization.ValueCommons.*;

final class ValueCommons {
    static final String TYPE = "type";

    static final Map<Class<? extends Enum<?>>, Function<String, ? extends Enum<?>>> ENUM_PARSERS = Map.of(
            Color.class, Color::from,
            Size.class, Size::from
    );

    static final Map<String, Class<? extends Enum<?>>> ENUM_TYPE_MAP =
            ENUM_PARSERS.keySet().stream().collect(Collectors.toMap(Class::getSimpleName, Function.identity()));


    static final Map<Class<?>, Function<String, ?>> CUSTOM_TYPE_PARSERS = Map.of(
            Price.class, Price::from
    );

    static final Map<String, Class<?>> CUSTOM_TYPE_MAP =
            CUSTOM_TYPE_PARSERS.keySet().stream().collect(Collectors.toMap(Class::getSimpleName, Function.identity()));
}

final class ValueSerializer extends JsonSerializer<Value> {
    @Override
    public void serialize(Value variable, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (variable == null) {
            gen.writeNull();
            return;
        }
        gen.writeStartObject();

        String discriminator = variable.getDiscriminator().getName();


        switch (variable) {
            case Blank ignored -> {
                //do nothing - {} is special case
            }
            case BooleanValue(var bool) -> gen.writeBooleanField(discriminator, bool);

            case CurrencyValue(var num) -> gen.writeNumberField(discriminator, num);
            case DoubleValue(var num) -> writeDouble(gen, num, discriminator);
            case IntegerValue(var num) -> gen.writeNumberField(discriminator, num);
            case LongValue(var num) -> gen.writeNumberField(discriminator, num);

            case DateValue(var dateTime) -> gen.writeStringField(discriminator,
                    dateTime.getHour() == 0 && dateTime.getMinute() == 0 && dateTime.getSecond() == 0 && dateTime.getNano() == 0
                            ? dateTime.toLocalDate().toString()  // yyyy-MM-dd
                            : dateTime.toString()  // ISO-8601 format (yyyy-MM-ddTHH:mm:ss)
            );
            case TimeValue(var time) -> gen.writeStringField(discriminator, time.toString());


            case ErrorValue(var message) -> gen.writeStringField(discriminator, message);
            case StringValue(var text) -> gen.writeStringField(discriminator, text);

            case CustomValue(var custom) -> {
                gen.writeStringField(discriminator, custom.toString());
                writeClass(gen, custom.getClass(), CUSTOM_TYPE_MAP);
            }
            case EnumValue(var enumVal) -> {
                gen.writeStringField(discriminator, enumVal.toString());
                writeClass(gen, enumVal.getClass(), ENUM_TYPE_MAP);
            }

            default -> throw new IllegalStateException(variable.getClass().getSimpleName() + " is not supported");
        }

        gen.writeEndObject();
    }

    private static void writeDouble(JsonGenerator gen, double value, String field) throws IOException {
        if (Double.isNaN(value)) gen.writeStringField(field, "NaN");
        else if (value == Double.POSITIVE_INFINITY) gen.writeStringField(field, "∞");
        else if (value == Double.NEGATIVE_INFINITY) gen.writeStringField(field, "-∞");
        else
            gen.writeNumberField(field, value);
    }


    private static void writeClass(JsonGenerator gen, Class<?> clazz, Map<String, ?> map) throws IOException {
        var typeName = map.entrySet().stream().filter(kvp -> clazz.equals(kvp.getValue())).findFirst()
                .orElseThrow(() -> new IllegalStateException(clazz.getSimpleName() + " is not supported")).getKey();
        gen.writeStringField(TYPE, typeName);
    }
}

final class ValueDeserializer extends JsonDeserializer<Value> {

    @Override
    public Value deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
        JsonNode node = p.readValueAsTree();
        if (!node.isObject())
            throw new JsonParseException(p, "Only object literals ('{ something }') are supported in Value schema");

        List<String> fields = convertIteratorToList(node.fieldNames());
        return switch (fields.size()) {
            case 0 -> Blank.BLANK; //by convention default value is Blank
            case 1 -> {
                String discriminatorText = fields.getFirst();
                var discriminator = ValueDiscriminator.fromName(discriminatorText);
                var valueNode = node.get(discriminatorText);

                Value ret = switch (discriminator) {
                    case BOOLEAN -> valueNode instanceof BooleanNode bn ? new BooleanValue(bn.booleanValue()) : null;

                    case CURRENCY -> valueNode instanceof NumericNode nn ? new CurrencyValue(nn.decimalValue()) : null;
                    case DOUBLE -> parseDouble(valueNode);
                    case INTEGER -> valueNode instanceof NumericNode nn ? new IntegerValue(nn.intValue()) : null;
                    case LONG -> valueNode instanceof NumericNode nn ? new LongValue(nn.longValue()) : null;

                    case ERROR -> valueNode instanceof TextNode tn ? new ErrorValue(tn.textValue()) : null;
                    case STRING -> valueNode instanceof TextNode tn ? new StringValue(tn.textValue()) : null;

                    case DATE ->
                            valueNode instanceof TextNode tn ? new DateValue(parseLocalDateTime(tn.textValue())) : null;
                    case TIME ->
                            valueNode instanceof TextNode tn ? new TimeValue(LocalTime.parse(tn.textValue())) : null;

                    default -> null;
                };
                if (ret == null)
                    throw schemaFail(p);
                else yield ret;
            }
            case 2 -> {
                if (!fields.contains(TYPE))
                    yield null;
                fields.remove(TYPE);

                var typeText = node.get(TYPE).asText();

                String discriminatorText = fields.getFirst();
                var discriminator = ValueDiscriminator.fromName(discriminatorText);
                var valueText = node.get(discriminatorText).asText();

                Value ret = switch (discriminator) {
                    case CUSTOM -> {
                        var clazz = CUSTOM_TYPE_MAP.get(typeText);
                        if (clazz == null) yield null;

                        var parser = CUSTOM_TYPE_PARSERS.get(clazz);
                        var parsed = parser.apply(valueText);
                        yield new CustomValue<>(parsed);
                    }

                    case ENUM -> {
                        var clazz = ENUM_TYPE_MAP.get(typeText);
                        if (clazz == null) yield null;

                        var parser = ENUM_PARSERS.get(clazz);
                        var parsed = parser.apply(valueText);
                        yield new EnumValue<>(parsed);
                    }

                    default -> null;
                };
                if (ret == null)
                    throw schemaFail(p);
                else yield ret;
            }
            default -> throw schemaFail(p);
        };
    }

    private static DoubleValue parseDouble(JsonNode valueNode) {
        return switch (valueNode) {
            case NumericNode nn -> new DoubleValue(nn.doubleValue());
            case TextNode tn when tn.asText() instanceof String text -> {
                double d = switch (text.trim().toLowerCase()) {
                    case "nan" -> Double.NaN;
                    case "∞", "+∞" -> Double.POSITIVE_INFINITY;
                    case "-∞" -> Double.NEGATIVE_INFINITY;
                    default -> Double.parseDouble(text);
                };
                yield new DoubleValue(d);
            }
            case null, default -> null;
        };
    }


    private static LocalDateTime parseLocalDateTime(String text) {
        return text.contains("T") ? LocalDateTime.parse(text) : LocalDate.parse(text).atStartOfDay();
    }

    private JacksonException schemaFail(JsonParser p) {
        return new JsonParseException(p, """
                Invalid schema for Value. Supported schema are:
                {} -> blank
                {"boolean": true or false}
                {"currency|double": floating point number}
                {"integer|long": integer number}
                {"error": "error message"}
                {"string": "text"}
                {"date": "yyyy-MM-dd or ISO date-time"}
                {"time": "HH:mm:ss or HH:mm"}
                {"custom|enum": "VALUE", "type": "SIMPLE CLASS NAME"}
                """);
    }

    private static List<String> convertIteratorToList(Iterator<String> iterator) {
        List<String> list = new ArrayList<>();
        while (iterator.hasNext())
            list.add(iterator.next());
        return list;
    }
}

