package org.example;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.*;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.NonNull;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

//TO BE REMOVED - START
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes({
        @JsonSubTypes.Type(value = MyStringColumnDefinition.class, name = "string"),
        @JsonSubTypes.Type(value = MyDateColumnDefinition.class, name = "date")
})
interface MyColumnDefinitionPlaceHolder {
}

record MyStringColumnDefinition(String format) implements MyColumnDefinitionPlaceHolder {
}

record MyDateColumnDefinition(String format) implements MyColumnDefinitionPlaceHolder {
}
//TO BE REMOVED - END

@JsonSerialize(using = FieldTranslationSerializer.class)
@JsonDeserialize(using = FieldTranslationDeserializer.class)
public record FieldTranslation(@lombok.NonNull String from, @lombok.NonNull String to,
                               MyColumnDefinitionPlaceHolder columnDefinition) {

    public FieldTranslation(@NonNull String from, @NonNull String to) {
        this(from, to, null);
    }

    public static final String COLUMN_DEFINITION_FIELD = "columnDefinition";
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

        if (value.columnDefinition() instanceof MyColumnDefinitionPlaceHolder colDef)
            gen.writeObjectField(FieldTranslation.COLUMN_DEFINITION_FIELD, colDef);

        gen.writeEndObject();
    }
}

class FieldTranslationDeserializer extends JsonDeserializer<FieldTranslation> {

    @SuppressWarnings("ConstantValue")
    @Override
    public FieldTranslation deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
        JsonNode node = p.readValueAsTree();

        final String colDefField = FieldTranslation.COLUMN_DEFINITION_FIELD;

        if (node.isObject() && convertIteratorToList(node.fieldNames()) instanceof List<String> fields && fields.size() <= 2) {
            MyColumnDefinitionPlaceHolder colDef = null;
            if (fields.contains(colDefField)) {
                colDef = ctx.readTreeAsValue(node.get(colDefField), MyColumnDefinitionPlaceHolder.class);
                fields.remove(colDefField);
            }
            if (fields.size() != 1)
                throw new JsonParseException(p, "Only one from-to field is supported i.e. { \"from\": \"to\" } in addition to optional column definition");

            String from = fields.getFirst();

            var toNode = node.get(from);

            String to;
            if (toNode instanceof TextNode textNode)
                to = textNode.asText();
            else throw new JsonParseException(p, "Only text value nodes are supported");

            return new FieldTranslation(from, to, colDef);
        } else throw new JsonParseException(p, """
                Invalid schema for FieldTranslation. Supported schema is {"oldField": "translation", "columnDefinition": {...optional column definition}}
                """);
    }

    private static List<String> convertIteratorToList(Iterator<String> iterator) {
        List<String> list = new ArrayList<>();
        while (iterator.hasNext())
            list.add(iterator.next());
        return list;
    }
}