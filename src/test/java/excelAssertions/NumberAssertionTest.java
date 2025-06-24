package excelAssertions;

import org.assertj.core.api.AbstractDoubleAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.data.Offset;
import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class NumberAssertionTest {

    @ParameterizedTest
    @MethodSource("passingCases")
    void shouldPassAssertion(NumberAssertion assertion, double actual) {
        AbstractDoubleAssert<?> base = assertThat(actual);
        assertion.apply(base); // should not throw
    }

    @ParameterizedTest
    @MethodSource("failingCases")
    void shouldFailAssertion(NumberAssertion assertion, double actual) {
        assertThatThrownBy(() -> assertion.apply(assertThat(actual)))
                .isInstanceOf(AssertionError.class);
    }

    static Stream<Arguments> passingCases() {
        return Stream.of(
                arguments(new EqualToNumberAssertion(5.5), 5.5), // ==5.5
                arguments(new GreaterThanNumberAssertion(3.0), 3.1), // >3.0
                arguments(new GreaterThanOrEqualToNumberAssertion(2.0), 2.0), // ≥2.0
                arguments(new LessThanNumberAssertion(10.0), 9.9), // <10.0
                arguments(new LessThanOrEqualToNumberAssertion(10.0), 10.0), // ≤10.0
                arguments(new CloseToOffsetNumberAssertion(5.0, Offset.offset(0.5)), 5.4), // ≈5.0±0.5
                arguments(new CloseToPercentNumberAssertion(100.0, Percentage.withPercentage(5)), 104.0), // ≈100.0±5%
                arguments(new WithinRangeNumberAssertion(1, 5), 1.0), // ∈ [1..5]
                arguments(new WithinRangeNumberAssertion(1, 5, true, true), 3.0), // ∈ (1..5)
                arguments(new OutsideRangeNumberAssertion(1, 5), 0.9), // ∉ [1..5]
                arguments(new OutsideRangeNumberAssertion(1, 5, true, true), 1.0), // ∉ (1..5)
                arguments(new OutsideRangeNumberAssertion(1, 5, true, true), 5.0) // ∉ (1..5)
        );
    }

    static Stream<Arguments> failingCases() {
        return Stream.of(
                arguments(new EqualToNumberAssertion(5.5), 5.4), // ==5.5
                arguments(new GreaterThanNumberAssertion(3.0), 2.9), // >3.0
                arguments(new GreaterThanOrEqualToNumberAssertion(2.0), 1.9), // ≥2.0
                arguments(new LessThanNumberAssertion(10.0), 10.1), // <10.0
                arguments(new LessThanOrEqualToNumberAssertion(10.0), 10.1), // ≤10.0
                arguments(new CloseToOffsetNumberAssertion(5.0, Offset.offset(0.5)), 5.6), // ≈5.0±0.5
                arguments(new CloseToPercentNumberAssertion(100.0, Percentage.withPercentage(5)), 106.0), // ≈100.0±5%
                arguments(new WithinRangeNumberAssertion(1, 5), 0.99), // ∈ [1..5]
                arguments(new WithinRangeNumberAssertion(1, 5, true, true), 1.0), // ∈ (1..5)
                arguments(new OutsideRangeNumberAssertion(1, 5), 3.0), // ∉ [1..5]
                arguments(new OutsideRangeNumberAssertion(1, 5, true, true), 3.0) // ∉ (1..5)
        );
    }
}
