package conditionalFormattingExcel;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

@JsonSerialize(using = Format.FormatSerializer.class)
@JsonDeserialize(using = Format.FormatDeserializer.class)
public sealed interface Format permits Format.PercentFormat, Format.FixedFormat, Format.CurrencyFormat {
    String getPoiFormatString();

    record PercentFormat(int decimalPlaces) implements Format {
        public PercentFormat {
            if (decimalPlaces < 0) {
                throw new IllegalArgumentException("decimalPlaces must be non-negative");
            }
        }

        @Override
        public String getPoiFormatString() {
            return "#,##0" + (decimalPlaces > 0 ? "." + "0".repeat(decimalPlaces) : "") + "%";
        }
    }

    record FixedFormat(int decimalPlaces) implements Format {
        public FixedFormat {
            if (decimalPlaces < 0) throw new IllegalArgumentException("decimalPlaces must be non-negative");
        }

        @Override
        public String getPoiFormatString() {
            return "#,##0" + (decimalPlaces > 0 ? "." + "0".repeat(decimalPlaces) : "");
        }
    }

    record CurrencyFormat(int decimalPlaces, String currencySymbol) implements Format {
        public CurrencyFormat {
            if (decimalPlaces < 0) throw new IllegalArgumentException("decimalPlaces must be non-negative");
            if (currencySymbol == null || currencySymbol.isBlank())
                throw new IllegalArgumentException("currencySymbol must not be null or blank");
        }

        @Override
        public String getPoiFormatString() {
            return "[$" + currencySymbol + "]#,##0" +
                   (decimalPlaces > 0 ? "." + "0".repeat(decimalPlaces) : "");
        }
    }

    final class FormatSerializer extends JsonSerializer<Format> {
        @Override
        public void serialize(Format value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value == null) {
                gen.writeNull();
                return;
            }
            gen.writeStartObject();
            switch (value) {
                case PercentFormat p -> gen.writeNumberField("percent", p.decimalPlaces());
                case FixedFormat f -> gen.writeNumberField("fixed", f.decimalPlaces());
                case CurrencyFormat c -> gen.writeStringField("currency", c.decimalPlaces() + ";" + c.currencySymbol());
                default -> throw new JsonMappingException(gen, "Unknown Format type: " + value.getClass());
            }
            gen.writeEndObject();
        }
    }

    final class FormatDeserializer extends JsonDeserializer<Format> {
        @Override
        public Format deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.readValueAsTree();
            if (node.isNull())
                return null;

            if (!node.isObject())
                throw JsonMappingException.from(p, "Expected JSON object for Format");

            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();

            if (!fields.hasNext()) return new FixedFormat(2); // Empty object

            Map.Entry<String, JsonNode> firstField = fields.next();

            if (fields.hasNext()) {
                throw JsonMappingException.from(p, "Format object must contain at most one field: 'percent', 'fixed', or 'currency'");
            }

            String key = firstField.getKey();
            JsonNode valueNode = firstField.getValue();

            return switch (key) {
                case "percent" -> {
                    if (!valueNode.isInt()) throw JsonMappingException.from(p, "'percent' must be an integer");
                    yield new PercentFormat(valueNode.asInt());
                }
                case "fixed" -> {
                    if (!valueNode.isInt()) throw JsonMappingException.from(p, "'fixed' must be an integer");
                    yield new FixedFormat(valueNode.asInt());
                }
                case "currency" -> {
                    if (!valueNode.isTextual() || valueNode.asText() == null || valueNode.asText().isBlank())
                        throw JsonMappingException.from(p, "'currency' must be a non blank string in format 'decimalPlaces;symbol'");

                    String[] parts = valueNode.asText().split(";", 2);
                    if (parts.length != 2)
                        throw JsonMappingException.from(p, "Invalid currency format: expected 'decimalPlaces;symbol'");
                    int decimalPlaces;
                    try {
                        decimalPlaces = Integer.parseInt(parts[0]);
                    } catch (NumberFormatException e) {
                        throw JsonMappingException.from(p, "Invalid decimalPlaces in currency format: " + parts[0]);
                    }

                    String symbol = parts[1];
                    if (symbol == null || symbol.isBlank())
                        throw JsonMappingException.from(p, "currencySymbol must not be null or blank");

                    yield new CurrencyFormat(decimalPlaces, symbol);
                }
                default -> throw JsonMappingException.from(p, "Unknown format key: " + key);
            };
        }
    }
}
