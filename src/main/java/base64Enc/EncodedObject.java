package base64Enc;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

@JsonSerialize(using = EncodedObject.EncodedObjectSerializer.class)
@JsonDeserialize(using = EncodedObject.EncodedObjectDeserializer.class)
public record EncodedObject(Object value) {
    public enum ValueType {
        NULL, SHORT, INTEGER, LONG, FLOAT, DOUBLE, BIG_INTEGER, BIG_DECIMAL, BOOL, STRING
    }

    public EncodedObject {
        //TODO move to special logix
        if (value != null && !(value instanceof String || value instanceof Boolean || value instanceof Number))
            throw new IllegalArgumentException("Only String, Boolean, or Number types are supported.");
    }

    public ValueType getValueType() {
        return switch (value) {
            case null -> ValueType.NULL;
            case Short ignored -> ValueType.SHORT;
            case Integer ignored -> ValueType.INTEGER;
            case Long ignored -> ValueType.LONG;
            case Float ignored -> ValueType.FLOAT;
            case Double ignored -> ValueType.DOUBLE;
            case BigInteger ignored -> ValueType.BIG_INTEGER;
            case BigDecimal ignored -> ValueType.BIG_DECIMAL;
            case Boolean ignored -> ValueType.BOOL;
            case String ignored -> ValueType.STRING;
            default -> throw new IllegalStateException("Unknown type: " + value.getClass());
        };
    }

    static class EncodedObjectSerializer extends JsonSerializer<EncodedObject> {
        @Override
        public void serialize(EncodedObject encoded, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            Object value = encoded.value();
            switch (value) {
                case null -> gen.writeNull();
                case String str -> gen.writeString(EncodedString.encode(str));
                case Boolean bool -> gen.writeBoolean(bool);
                case Short s -> gen.writeNumber(s);
                case Integer i -> gen.writeNumber(i);
                case Long l -> gen.writeNumber(l);
                case Float f -> gen.writeNumber(f);
                case Double d -> gen.writeNumber(d);
                case BigInteger bi -> gen.writeNumber(bi);
                case BigDecimal bd -> gen.writeNumber(bd);
                default -> throw new IllegalStateException("Unexpected value type: " + value.getClass());
            }
        }
    }

    static class EncodedObjectDeserializer extends JsonDeserializer<EncodedObject> {
        @Override
        public EncodedObject deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            JsonToken token = p.currentToken();

            Object obj = switch (token) {
                case VALUE_NULL -> null;
                case VALUE_STRING -> EncodedString.decode(p.getText());
                case VALUE_TRUE, VALUE_FALSE -> p.getBooleanValue();
                case VALUE_NUMBER_INT -> {
                    String numStr = p.getText();
                    try {
                        yield Short.parseShort(numStr);
                    } catch (NumberFormatException e1) {
                        try {
                            yield Integer.parseInt(numStr);
                        } catch (NumberFormatException e2) {
                            try {
                                yield Long.parseLong(numStr);
                            } catch (NumberFormatException e3) {
                                yield new BigInteger(numStr);
                            }
                        }
                    }
                }
                case VALUE_NUMBER_FLOAT -> {
                    String numStr = p.getText();
                    try {
                        var d = Double.parseDouble(numStr);
                        yield !Double.isInfinite(d) ? d : new BigDecimal(numStr);
                    } catch (NumberFormatException e) {
                        yield new BigDecimal(numStr);
                    }
                }
                default -> throw JsonMappingException.from(p, "Unsupported JSON token for EncodedObject: " + token);
            };

            return new EncodedObject(obj);
        }
    }
}
