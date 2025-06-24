package excelAssertions;

import org.assertj.core.api.AbstractDoubleAssert;
import org.assertj.core.data.Offset;
import org.assertj.core.data.Percentage;

sealed interface NumberAssertion permits CloseToOffsetNumberAssertion, CloseToPercentNumberAssertion, EqualToNumberAssertion, GreaterThanNumberAssertion,
        GreaterThanOrEqualToNumberAssertion, LessThanNumberAssertion, LessThanOrEqualToNumberAssertion, WithinRangeNumberAssertion, OutsideRangeNumberAssertion {

    void apply(AbstractDoubleAssert<? extends AbstractDoubleAssert<?>> assertion);

    @Override
    String toString();
}

record EqualToNumberAssertion(double expected) implements NumberAssertion {
    @Override
    public void apply(AbstractDoubleAssert<? extends AbstractDoubleAssert<?>> assertion) {
        assertion.isEqualTo(expected);
    }

    @Override
    public String toString() {
        return "== " + expected;
    }
}

record GreaterThanNumberAssertion(double threshold) implements NumberAssertion {
    @Override
    public void apply(AbstractDoubleAssert<? extends AbstractDoubleAssert<?>> assertion) {
        assertion.isGreaterThan(threshold);
    }

    @Override
    public String toString() {
        return "> " + threshold;
    }
}

record GreaterThanOrEqualToNumberAssertion(double threshold) implements NumberAssertion {
    @Override
    public void apply(AbstractDoubleAssert<? extends AbstractDoubleAssert<?>> assertion) {
        assertion.isGreaterThanOrEqualTo(threshold);
    }

    @Override
    public String toString() {
        return ">= " + threshold;
    }
}

record LessThanNumberAssertion(double threshold) implements NumberAssertion {
    @Override
    public void apply(AbstractDoubleAssert<? extends AbstractDoubleAssert<?>> assertion) {
        assertion.isLessThan(threshold);
    }

    @Override
    public String toString() {
        return "< " + threshold;
    }
}

record LessThanOrEqualToNumberAssertion(double threshold) implements NumberAssertion {
    @Override
    public void apply(AbstractDoubleAssert<? extends AbstractDoubleAssert<?>> assertion) {
        assertion.isLessThanOrEqualTo(threshold);
    }

    @Override
    public String toString() {
        return "<= " + threshold;
    }
}

record CloseToOffsetNumberAssertion(double expected, Offset<Double> offset) implements NumberAssertion {
    @Override
    public void apply(AbstractDoubleAssert<? extends AbstractDoubleAssert<?>> assertion) {
        assertion.isCloseTo(expected, offset);
    }

    @Override
    public String toString() {
        return "~" + expected + "±" + offset.value;
    }
}

record CloseToPercentNumberAssertion(double expected, Percentage percentage) implements NumberAssertion {
    @Override
    public void apply(AbstractDoubleAssert<? extends AbstractDoubleAssert<?>> assertion) {
        assertion.isCloseTo(expected, percentage);
    }

    @Override
    public String toString() {
        return "~" + expected + "±" + percentage.value + "%";
    }
}

record WithinRangeNumberAssertion(double from, double to, boolean exclusiveFrom,
                                  boolean exclusiveTo) implements NumberAssertion {
    public WithinRangeNumberAssertion {
        if (to < from) throw new IllegalArgumentException("to must be >= from");
    }

    public WithinRangeNumberAssertion(double from, double to) {
        this(from, to, false, false);
    }

    @Override
    public void apply(AbstractDoubleAssert<? extends AbstractDoubleAssert<?>> assertion) {
        if (exclusiveFrom) assertion.isGreaterThan(from);
        else assertion.isGreaterThanOrEqualTo(from);

        if (exclusiveTo) assertion.isLessThan(to);
        else assertion.isLessThanOrEqualTo(to);
    }

    @Override
    public String toString() {
        String left = exclusiveFrom ? "(" : "[";
        String right = exclusiveTo ? ")" : "]";
        return "∈ " + left + from + ".." + to + right;
    }
}

record OutsideRangeNumberAssertion(double from, double to, boolean exclusiveFrom,
                                   boolean exclusiveTo) implements NumberAssertion {
    public OutsideRangeNumberAssertion {
        if (to < from) throw new IllegalArgumentException("to must be >= from");
    }

    public OutsideRangeNumberAssertion(double from, double to) {
        this(from, to, false, false);
    }

    @Override
    public void apply(AbstractDoubleAssert<? extends AbstractDoubleAssert<?>> assertion) {
        assertion.satisfies(actual -> {
            boolean left = exclusiveFrom ? actual <= from : actual < from;
            boolean right = exclusiveTo ? actual >= to : actual > to;
            if (!(left || right)) {
                throw new AssertionError("Expected number to be " + toString() + " but was " + actual);
            }
        });
    }

    @Override
    public String toString() {
        String left = exclusiveFrom ? "(" : "[";
        String right = exclusiveTo ? ")" : "]";
        return "∉ " + left + from + ".." + to + right;
    }
}
