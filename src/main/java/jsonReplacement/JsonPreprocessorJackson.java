package jsonReplacement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonPreprocessorJackson {

    private static final Pattern REPLACEMENT_PATTERN = Pattern.compile("\\$REPLACEMENT:([^\\$]+)\\$");
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JsonNode preprocess(String jsonString, Map<String, Object> replacements) throws IOException {
        if (jsonString == null || jsonString.isEmpty()) {
            throw new IllegalArgumentException("JSON string cannot be null or empty.");
        }

        JsonNode rootNode = objectMapper.readTree(jsonString);
        return processNode(rootNode, replacements);
    }

    private JsonNode processNode(JsonNode node, Map<String, Object> replacements) {
        if (node.isObject()) {
            return processObjectNode((ObjectNode) node, replacements);
        } else if (node.isTextual()) {
            return processTextNode(node.asText(), replacements);
        } else {
            return node; // Return as is for other node types
        }
    }

    private JsonNode processObjectNode(ObjectNode objectNode, Map<String, Object> replacements) {
        Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
        ObjectNode processedNode = objectMapper.createObjectNode();

        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            processedNode.set(field.getKey(), processNode(field.getValue(), replacements));
        }

        return findAndReplaceObject(processedNode, replacements);
    }

    private JsonNode processTextNode(String text, Map<String, Object> replacements) {
        Matcher matcher = REPLACEMENT_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String replacementKey = matcher.group(1);
            if (replacements.containsKey(replacementKey) && replacements.get(replacementKey) instanceof String) {
                matcher.appendReplacement(sb, replacements.get(replacementKey).toString());
            } else {
                matcher.appendReplacement(sb, matcher.group(0)); // Keep original if no replacement
            }
        }
        matcher.appendTail(sb);
        return new TextNode(sb.toString());
    }

    private JsonNode findAndReplaceObject(ObjectNode objectNode, Map<String, Object> replacements) {
        Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
        ObjectNode resultNode = objectNode.deepCopy(); // Create a copy to modify

        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String key = field.getKey();
            JsonNode value = field.getValue();

            if (value.isObject()) {
                ObjectNode innerObject = (ObjectNode) value;
                if (innerObject.size() == 1 && innerObject.has("$REPLACEMENT")) {
                    String replacementKey = innerObject.get("$REPLACEMENT").asText();
                    if (replacements.containsKey(replacementKey)) {
                        resultNode.set(key, objectMapper.valueToTree(replacements.get(replacementKey)));
                    }
                } else {
                    resultNode.set(key, findAndReplaceObject(innerObject, replacements));
                }
            }
        }
        return resultNode;
    }
}
