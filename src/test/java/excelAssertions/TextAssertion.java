package excelAssertions;

import org.assertj.core.api.AbstractStringAssert;

import java.util.Comparator;
import java.util.regex.Pattern;

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

    protected abstract void assertOnValue(AbstractStringAssert<? extends AbstractStringAssert<?>> assertion);
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
    protected void assertOnValue(AbstractStringAssert<? extends AbstractStringAssert<?>> assertion) {
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

    public ContainsTextAssertion(String expectedSubstring, boolean ignoreCase) {
        super(ignoreCase);
        this.expectedSubstring = expectedSubstring;
    }

    @Override
    protected void assertOnValue(AbstractStringAssert<? extends AbstractStringAssert<?>> assertion) {
        if (this.ignoreCase) assertion.containsIgnoringCase(this.expectedSubstring);
        else assertion.contains(this.expectedSubstring);
    }
}

final class PatternTextAssertion extends TextAssertion<PatternTextAssertion> {
    final String regex;
    final boolean singleLineMode;

    public PatternTextAssertion(String regex, boolean ignoreCase, boolean singleLineMode) {
        super(ignoreCase);
        this.regex = regex;
        this.singleLineMode = singleLineMode;
    }

    @Override
    protected void assertOnValue(AbstractStringAssert<?> assertion) {
        int flags = 0;
        if (this.ignoreCase) flags |= Pattern.CASE_INSENSITIVE;
        if (this.singleLineMode) flags |= Pattern.DOTALL;

        Pattern pattern = Pattern.compile(regex, flags);
        assertion.matches(pattern);
    }
}