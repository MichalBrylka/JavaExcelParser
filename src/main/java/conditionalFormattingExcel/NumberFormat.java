package conditionalFormattingExcel;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.IntNode;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

@com.fasterxml.jackson.databind.annotation.JsonSerialize(using = NumberFormat.NumberFormatSerializer.class)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = NumberFormat.NumberFormatDeserializer.class)
public record NumberFormat(int fixedDecimalPlaces, int percentDecimalPlaces, int currencyDecimalPlaces) {
    public NumberFormat {
        if (fixedDecimalPlaces < 0) {
            throw new IllegalArgumentException("fixedDecimalPlaces must be non-negative");
        }
        if (percentDecimalPlaces < 0) {
            throw new IllegalArgumentException("percentDecimalPlaces must be non-negative");
        }
        if (currencyDecimalPlaces < 0) {
            throw new IllegalArgumentException("currencyDecimalPlaces must be non-negative");
        }
    }

    public NumberFormat(Integer fixedDecimalPlaces, Integer percentDecimalPlaces, Integer currencyDecimalPlaces) {
        this(
                fixedDecimalPlaces == null ? 2 : fixedDecimalPlaces,
                percentDecimalPlaces == null ? 0 : percentDecimalPlaces,
                currencyDecimalPlaces == null ? 2 : currencyDecimalPlaces
        );
    }

    public static final class NumberFormatSerializer extends JsonSerializer<NumberFormat> {
        @Override
        public void serialize(NumberFormat value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value == null) {
                gen.writeNull();
                return;
            }
            gen.writeStartObject();
            gen.writeNumberField("#", value.fixedDecimalPlaces());
            gen.writeNumberField("%", value.percentDecimalPlaces());
            gen.writeNumberField("$", value.currencyDecimalPlaces());
            gen.writeEndObject();
        }
    }

    public static final class NumberFormatDeserializer extends JsonDeserializer<NumberFormat> {
        private static final Set<String> ALLOWED_KEYS = Set.of("#", "%", "$");

        @Override
        public NumberFormat deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            JsonNode node = p.readValueAsTree();
            if (node.isNull())
                return null;

            if (!node.isObject())
                throw JsonMappingException.from(p, "Expected JSON object for Format");

            // Validate no unknown fields exist
            Iterator<String> fieldNames = node.fieldNames();
            while (fieldNames.hasNext()) {
                String field = fieldNames.next();
                if (!ALLOWED_KEYS.contains(field))
                    throw JsonMappingException.from(p, "Unknown field: \"" + field + "\". Allowed fields are: " + ALLOWED_KEYS);
            }

            Integer fixed = node.get("#") instanceof IntNode fixedNode ? fixedNode.intValue() : null;
            Integer percent = node.get("%") instanceof IntNode percentNode ? percentNode.intValue() : null;
            Integer currency = node.get("$") instanceof IntNode currencyNode ? currencyNode.intValue() : null;

            return new NumberFormat(fixed, percent, currency);
        }
    }
}
