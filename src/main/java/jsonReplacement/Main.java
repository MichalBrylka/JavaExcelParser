package jsonReplacement;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.*;

public class Main {
    public static void main(String[] args) throws IOException {
        String jsonString = """
                {
                  "message": "Hello $REPLACEMENT:name$, you are $REPLACEMENT:age$ years old.",
                  "user": {
                    "$REPLACEMENT": "userObject"
                  },
                  "nested": {
                    "message2": "$REPLACEMENT:message2$"
                  },
                  "invalid": {
                    "a": 1,
                    "$REPLACEMENT": "test"
                  }
                }
                """;

        Map<String, Object> replacements = Map.of(
                "name", "Alice",
                "age", "30",
                "userObject", Map.of("username", "bob"),
                "message2", "Nested replacement"
        );

        JsonPreprocessorJackson preprocessor = new JsonPreprocessorJackson();
        JsonNode processedJson = preprocessor.preprocess(jsonString, replacements);

        System.out.println(processedJson.toString());
        //OUTPUT:
        //{"message":"Hello Alice, you are 30 years old.","user":{"username":"bob"},"nested":{"message2":"Nested replacement"},"invalid":{"a":1,"$REPLACEMENT":"test"}}
    }
}