package rangeCust;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@com.fasterxml.jackson.databind.annotation.JsonSerialize(using = IndexSource.IndexSourceSerializer.class)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = IndexSource.IndexSourceDeserializer.class)
public sealed interface IndexSource permits IndexSource.CombiningRange, IndexSource.Empty, IndexSource.Range, IndexSource.SingleIndex, IndexSource.WholeRange {
    boolean canResolve(int listSize);

    <T> List<T> resolve(List<T> list);

    static IndexSource single(int index) {
        return new SingleIndex(index);
    }

    static IndexSource range(Integer start, Integer end) {
        return new Range(start, end);
    }

    static IndexSource all() {
        return WholeRange.INSTANCE;
    }

    static IndexSource empty() {
        return Empty.INSTANCE;
    }

    static IndexSource from(List<Integer> elements) {
        return new CombiningRange(elements.stream().map(i -> (IndexSource) new SingleIndex(i)).toList());
    }

    private static int normalize(int index, int size) {
        return index < 0 ? size + index : index;
    }

    record SingleIndex(int index) implements IndexSource {
        @Override
        public boolean canResolve(int listSize) {
            int normalized = normalize(index, listSize);
            return normalized >= 0 && normalized < listSize;
        }

        @Override
        public <T> List<T> resolve(List<T> list) {
            int normalized = normalize(index, list.size());
            return List.of(list.get(normalized));
        }

        @Override
        public String toString() {
            return "[%d]".formatted(index);
        }
    }

    record Range(Integer start, Integer end) implements IndexSource {
        @Override
        public boolean canResolve(int listSize) {
            int start = (this.start == null) ? 0 : normalize(this.start, listSize);
            int end = (this.end == null) ? listSize - 1 : normalize(this.end, listSize);
            return start >= 0 && end >= 0 && start <= end && end < listSize;
        }

        @Override
        public <T> List<T> resolve(List<T> list) {
            int start = (this.start == null) ? 0 : normalize(this.start, list.size());
            int end = (this.end == null) ? list.size() - 1 : normalize(this.end, list.size());
            return IntStream.rangeClosed(start, end).mapToObj(list::get).toList();
        }

        @Override
        public String toString() {
            return "[" + (start == null ? "" : start) + ", " + (end == null ? "" : end) + ']';
        }
    }

    final class WholeRange implements IndexSource {
        private WholeRange() {
        }

        public static IndexSource INSTANCE = new WholeRange();

        @Override
        public boolean canResolve(int listSize) {
            return true;
        }

        @Override
        public <T> List<T> resolve(List<T> list) {
            return list == null || list.isEmpty()
                    ? List.of()
                    : List.copyOf(list);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof WholeRange;
        }

        @Override
        public int hashCode() {
            return 1;
        }

        @Override
        public String toString() {
            return "ALL";
        }
    }

    final class Empty implements IndexSource {
        private Empty() {
        }

        public static IndexSource INSTANCE = new Empty();

        @Override
        public boolean canResolve(int listSize) {
            return true;
        }

        @Override
        public <T> List<T> resolve(List<T> list) {
            return List.of();
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Empty;
        }

        @Override
        public int hashCode() {
            return 2;
        }

        @Override
        public String toString() {
            return "EMPTY";
        }
    }

    record CombiningRange(@NotNull List<IndexSource> elements) implements IndexSource {
        @Override
        public boolean canResolve(int listSize) {
            return elements.stream().allMatch(e -> e.canResolve(listSize));
        }

        @Override
        public <T> List<T> resolve(List<T> list) {
            return elements.stream().flatMap(e -> e.resolve(list).stream()).toList();
        }

        @Override
        public String toString() {
            return "{" +
                   elements.stream().map(is -> is == null ? "NULL" : is.toString())
                           .collect(Collectors.joining(" ; "))
                   + '}';
        }
    }

    class IndexSourceSerializer extends JsonSerializer<IndexSource> {
        @Override
        public void serialize(IndexSource value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(serializeElement(value));
        }

        private static String serializeElement(IndexSource indexSource) {
            return switch (indexSource) {
                case Empty ignored -> "";
                case WholeRange ignored -> "..";
                case SingleIndex(int index) -> String.valueOf(index);
                case Range(var start, var end) -> (start != null ? start.toString() : "")
                                                  + ".." +
                                                  (end != null ? end.toString() : "");
                case CombiningRange(var elements) ->
                        elements.stream().map(IndexSourceSerializer::serializeElement).filter(t -> t != null && !t.isBlank()).collect(Collectors.joining(","));
                case null -> null;
            };
        }
    }

    class IndexSourceDeserializer extends JsonDeserializer<IndexSource> {
        @Override
        public IndexSource deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            JsonNode node = p.readValueAsTree();

            if (node.isNull()) return Empty.INSTANCE;

            if (!node.isTextual()) throw JsonMappingException.from(p, "Only text nodes are supported");

            var indexExpr = node.textValue();
            if (indexExpr == null || indexExpr.isBlank())
                return Empty.INSTANCE;

            List<IndexSource> elements = Arrays.stream(indexExpr.split(","))
                    .map(String::trim)
                    .map(IndexSourceDeserializer::parseElement)
                    .toList();

            if (elements.isEmpty()) return Empty.INSTANCE;
            else if (elements.size() == 1) return elements.getFirst();
            else return new CombiningRange(elements);
        }

        private static final Pattern PATTERN = Pattern.compile("""
                        (?<all>^\\.\\.$)
                        |(?<range>^(?<start>-?\\d+)?\\.\\.(?<end>-?\\d+)?$)
                        |(?<single>^-?\\d+$)
                        """,
                Pattern.COMMENTS);

        private static IndexSource parseElement(String input) {
            if (input == null || input.isBlank())
                return Empty.INSTANCE;

            var matcher = PATTERN.matcher(input);

            if (!matcher.matches())
                throw new IllegalArgumentException("Invalid expression: " + input);

            if (matcher.group("all") != null)
                return WholeRange.INSTANCE;

            if (matcher.group("range") != null)
                return new Range(
                        matcher.group("start") != null ? Integer.parseInt(matcher.group("start")) : null,
                        matcher.group("end") != null ? Integer.parseInt(matcher.group("end")) : null);

            if (matcher.group("single") != null)
                return new SingleIndex(Integer.parseInt(matcher.group("single")));

            throw new IllegalArgumentException("Unknown format: " + input);
        }
    }
}