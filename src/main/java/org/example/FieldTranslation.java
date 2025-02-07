package org.example;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.*;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.ValueNode;


import java.io.IOException;
import java.util.Iterator;

/*var objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        var orig = new FieldTranslation(" gff gfg ", "1234");
        String json = objectMapper.writeValueAsString(orig);
        var deser = objectMapper.readValue(json, FieldTranslation.class);*/

@JsonSerialize(using = FieldTranslationSerializer.class)
@JsonDeserialize(using = FieldTranslationDeserializer.class)
public record FieldTranslation(@lombok.NonNull String from, @lombok.NonNull String to) {
}

class FieldTranslationSerializer extends JsonSerializer<FieldTranslation> {

    @Override
    public void serialize(FieldTranslation value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null || value.from() == null || value.to() == null) {
            gen.writeNull();
            return;
        }
        gen.writeStartObject();
        gen.writeStringField(value.from(), value.to());
        gen.writeEndObject();
    }
}

class FieldTranslationDeserializer extends JsonDeserializer<FieldTranslation> {

    @Override
    public FieldTranslation deserialize(JsonParser p, DeserializationContext ctx) throws IOException, JacksonException {
        var node = p.getCodec().readTree(p);

        if (node.isObject() && node.fieldNames() instanceof Iterator<String> fields &&
                fields.hasNext() && fields.next() instanceof String firstField &&
                node.get(firstField) instanceof ValueNode valueNode) {
            if (fields.hasNext())
                throw new JsonParseException(p, "Too many fields. Exactly one field key:value is supported");

            String valueText;
            if (valueNode instanceof TextNode textNode)
                valueText = textNode.asText();
            else throw new JsonParseException(p, "Only text value nodes are supported");

            return new FieldTranslation(firstField, valueText);
        } else throw new JsonParseException(p, """
                Invalid schema for FieldTranslation. Supported schema is {"oldField": "translation"}
                """);
    }
}