package objectEnricher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@lombok.extern.log4j.Log4j2
public class Main {
    @lombok.SneakyThrows
    public static void main(String[] args) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        var json = mapper.writeValueAsString(new ObjectReplacement<Boolean>(Map.of("First", false, "Second", true), Boolean.class));

        json = """
                {
                  "replacement" : {
                    "Second" : true,
                    "First" : "xxx"
                  },
                  "clazz" : "java.lang.Boolean"
                }""";
        var deser = mapper.readValue(json, ObjectReplacement.class);

        log.info("Result: {}", json);
        log.info("Result: {}", deser);
        log.info("Result: {}", deser.replacement().getClass());
    }
}

record ObjectReplacement<T>(@NotNull Map<String, T> replacement, @NotNull Class<T> clazz) {
    ObjectReplacement {
        if (replacement == null || replacement.isEmpty())
            throw new IllegalArgumentException("replacement cannot be empty");

        // Ensure all non-null values are of the same type
        /*Class<?> expectedType = null;
        boolean hasMixedTypes = false;

        for (T value : replacement.values()) {
            if (value != null) {
                if (expectedType == null) {
                    expectedType = value.getClass();
                } else if (!expectedType.equals(value.getClass())) {
                    hasMixedTypes = true;
                    break;
                }
            }
        }

        if (hasMixedTypes) {
            // Group by type and collect keys
            String errorMessage = replacement.entrySet().stream()
                    .filter(entry -> entry.getValue() != null)
                    .collect(Collectors.groupingBy(
                            entry -> entry.getValue().getClass(),
                            Collectors.mapping(Map.Entry::getKey, Collectors.joining(", "))
                    ))
                    .entrySet().stream()
                    .map(entry -> " - Type " + entry.getKey().getName() + " for keys: " + entry.getValue())
                    .collect(Collectors.joining("\n", "All non-null values must be of the same type, but found:\n", ""));

            throw new IllegalArgumentException(errorMessage);
        }*/


        /*Class<?> expectedType = null;
        for (T value : replacement.values()) {
            if (value != null) {
                if (expectedType == null) {
                    expectedType = value.getClass(); // Set the first encountered type
                } else if (!expectedType.equals(value.getClass())) {
                    throw new IllegalArgumentException("All non-null values must be of the same type");
                }
            }
        }*/
    }
}