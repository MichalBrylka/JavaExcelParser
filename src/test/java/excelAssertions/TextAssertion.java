package excelAssertions;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import org.assertj.core.api.AbstractStringAssert;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

@com.fasterxml.jackson.databind.annotation.JsonSerialize(using = TextAssertion.TextAssertionSerializer.class)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = TextAssertion.TextAssertionDeserializer.class)
public sealed abstract class TextAssertion<TAssertion extends TextAssertion<TAssertion>> permits ContainsTextAssertion, EqualsTextAssertion, PatternTextAssertion {
    boolean ignoreCase;

    protected TextAssertion(boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }

    public TAssertion ignoreCase() {
        this.ignoreCase = true;
        return self();
    }

    public TAssertion caseSensitive() {
        this.ignoreCase = false;
        return self();
    }

    @SuppressWarnings("unchecked")
    protected TAssertion self() {
        return (TAssertion) this;
    }

    protected abstract void apply(AbstractStringAssert<? extends AbstractStringAssert<?>> assertion);

    private static final String IGNORE_CASE = "ignoreCase";
    private static final String IGNORE_NEW_LINES = "ignoreNewLines";
    private static final String SINGLE_LINE_MODE = "singleLineMode";

    private static final Set<String> ALL_OPERATION_KEYS = Set.of("eq", "=", "==", "has", "∋", "like");
    private static final Set<String> ALL_OPTION_KEYS = Set.of(IGNORE_CASE, IGNORE_NEW_LINES, SINGLE_LINE_MODE);
    private static final Set<String> ALL_ALLOWED_KEYS;

    static {
        Set<String> combined = new HashSet<>(ALL_OPERATION_KEYS);
        combined.addAll(ALL_OPTION_KEYS);
        ALL_ALLOWED_KEYS = Collections.unmodifiableSet(combined);
    }

    static class TextAssertionSerializer extends JsonSerializer<TextAssertion<?>> {
        @Override
        public void serialize(TextAssertion<?> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value == null) {
                gen.writeNull();
                return;
            }

            gen.writeStartObject();

            switch (value) {
                case EqualsTextAssertion eta -> {
                    gen.writeStringField("eq", eta.expected);
                    if (eta.ignoreNewLines) gen.writeBooleanField(IGNORE_NEW_LINES, true);
                }

                case ContainsTextAssertion cta -> gen.writeStringField("has", cta.expectedSubstring);

                case PatternTextAssertion pta -> {
                    gen.writeStringField("like", pta.pattern);
                    if (pta.singleLineMode) gen.writeBooleanField(SINGLE_LINE_MODE, true);
                }
                default -> throw new JsonMappingException(gen, "Unknown TextAssertion subtype: " + value.getClass());
            }

            if (value.ignoreCase) gen.writeBooleanField(IGNORE_CASE, true);

            gen.writeEndObject();
        }
    }

    static class TextAssertionDeserializer extends JsonDeserializer<TextAssertion<?>> {

        @Override
        public TextAssertion<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);

            // Validate all field names
            Iterator<String> fieldNames = node.fieldNames();
            Set<String> presentFields = new HashSet<>();
            while (fieldNames.hasNext()) {
                String field = fieldNames.next();
                if (!ALL_ALLOWED_KEYS.contains(field))
                    throw new JsonMappingException(p, "Unknown field: " + field);
                presentFields.add(field);
            }

            // Find exactly one operation
            List<String> presentOps = ALL_OPERATION_KEYS.stream()
                    .filter(presentFields::contains)
                    .toList();

            if (presentOps.size() != 1)
                throw new JsonMappingException(p, "Exactly one operation key must be present, found: " + presentOps);

            // Read boolean options with default `false`
            boolean ignoreCase = node.path(IGNORE_CASE).asBoolean(false);
            boolean ignoreNewLines = node.path(IGNORE_NEW_LINES).asBoolean(false);
            boolean singleLineMode = node.path(SINGLE_LINE_MODE).asBoolean(false);

            // Deserialize based on operation
            String op = presentOps.getFirst();
            if (!(node.get(op) instanceof JsonNode argumentNode) || argumentNode.isNull())
                throw new JsonMappingException(p, "The value for '%s' must not be null".formatted(op));

            return switch (op) {
                case "eq", "=", "==" -> {
                    String expected = argumentNode.asText();
                    yield new EqualsTextAssertion(expected, ignoreCase, ignoreNewLines);
                }
                case "has", "∋" -> {
                    String substring = argumentNode.asText();
                    yield new ContainsTextAssertion(substring, ignoreCase);
                }
                case "like" -> {
                    String pattern = argumentNode.asText();
                    yield new PatternTextAssertion(pattern, ignoreCase, singleLineMode);
                }
                default -> throw new JsonMappingException(p, "Unhandled operation key: " + op);
            };
        }
    }
}

final class EqualsTextAssertion extends TextAssertion<EqualsTextAssertion> {
    final String expected;
    boolean ignoreNewLines;

    public EqualsTextAssertion(String expected, boolean ignoreCase, boolean ignoreNewLines) {
        super(ignoreCase);
        this.expected = expected;
        this.ignoreNewLines = ignoreNewLines;
    }

    public EqualsTextAssertion ignoreNewLines() {
        this.ignoreNewLines = true;
        return this;
    }

    public EqualsTextAssertion respectNewLines() {
        this.ignoreNewLines = false;
        return this;
    }

    @Override
    protected void apply(AbstractStringAssert<? extends AbstractStringAssert<?>> assertion) {
        if (this.ignoreNewLines) {
            Comparator<String> baseComparator = (s1, s2) ->
                    normalizeNewLines(s1, this.ignoreCase).compareTo(
                            normalizeNewLines(s2, this.ignoreCase)
                    );

            assertion.usingComparator(Comparator.nullsFirst(baseComparator)).isEqualTo(this.expected);
        } else {
            if (this.ignoreCase) assertion.isEqualToIgnoringCase(this.expected);
            else assertion.isEqualTo(this.expected);
        }
    }

    private static String normalizeNewLines(String s, boolean ignoreCase) {
        if (s == null) return null;
        String withoutNewlines = s.replaceAll("\\R", " ")  // replaces any newline with a space
                .replaceAll("\\s+", " ") // collapse multiple spaces
                .trim(); // optional: remove leading/trailing spaces
        return ignoreCase ? withoutNewlines.toLowerCase() : withoutNewlines;
    }
}

final class ContainsTextAssertion extends TextAssertion<ContainsTextAssertion> {
    final String expectedSubstring;

    @SuppressWarnings("ConstantValue")
    public ContainsTextAssertion(@NotNull String expectedSubstring, boolean ignoreCase) {
        super(ignoreCase);
        if (expectedSubstring == null)
            throw new IllegalArgumentException("expectedSubstring cannot be null");
        this.expectedSubstring = expectedSubstring;
    }

    @Override
    protected void apply(AbstractStringAssert<? extends AbstractStringAssert<?>> assertion) {
        if (this.ignoreCase) assertion.containsIgnoringCase(this.expectedSubstring);
        else assertion.contains(this.expectedSubstring);
    }
}

final class PatternTextAssertion extends TextAssertion<PatternTextAssertion> {
    final String pattern;
    boolean singleLineMode;

    public PatternTextAssertion singleLineMode() {
        this.singleLineMode = true;
        return this;
    }

    public PatternTextAssertion multiLineMode() {
        this.singleLineMode = false;
        return this;
    }

    @SuppressWarnings("ConstantValue")
    public PatternTextAssertion(@NotNull String pattern, boolean ignoreCase, boolean singleLineMode) {
        super(ignoreCase);
        if (pattern == null)
            throw new IllegalArgumentException("pattern cannot be null");

        this.pattern = pattern;
        this.singleLineMode = singleLineMode;
    }

    @Override
    protected void apply(AbstractStringAssert<?> assertion) {
        int flags = 0;
        if (this.ignoreCase) flags |= Pattern.CASE_INSENSITIVE;
        if (this.singleLineMode) flags |= Pattern.DOTALL;

        Pattern pattern = Pattern.compile(this.pattern, flags);
        assertion.matches(pattern);
    }
}