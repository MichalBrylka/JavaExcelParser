package valueSerialization;

import java.util.function.Function;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.*;
import com.fasterxml.jackson.databind.node.TextNode;

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

//TODO write test to check if enum/customs have toString and from() methods

final class ValueSerializer extends JsonSerializer<Value> {
    @Override
    public void serialize(Value variable, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (variable == null) {
            gen.writeNull();
            return;
        }
        gen.writeStartObject();

        String discriminator = variable.getDiscriminator().toString();


        switch (variable) {
            case Blank blank -> {
                //do nothing - {} is special case
            }
            case BooleanValue(var bool) -> gen.writeBooleanField(discriminator, bool);

            case CurrencyValue(var num) -> gen.writeNumberField(discriminator, num);
            case DoubleValue(var num) -> gen.writeNumberField(discriminator, num);
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
                writeClass(gen, custom.getClass(), ValueCommons.CUSTOM_TYPE_MAP);
            }
            case EnumValue(var enumVal) -> {
                gen.writeStringField(discriminator, enumVal.toString());
                writeClass(gen, enumVal.getClass(), ValueCommons.ENUM_TYPE_MAP);
            }

            default -> throw new IllegalStateException(variable.getClass().getSimpleName() + " is not supported");
        }

        gen.writeEndObject();
    }


    private static void writeClass(JsonGenerator gen, Class<?> clazz, Map<String, ?> map) throws IOException {
        var typeName = map.entrySet().stream().filter(kvp -> clazz.equals(kvp.getValue())).findFirst().orElseThrow(() -> new IllegalStateException(clazz.getSimpleName() + " is not supported")).getKey();
        gen.writeStringField(ValueCommons.TYPE, typeName);
    }
}