package excelAssertions;

import org.junit.jupiter.api.Test;

import static java.lang.Math.nextAfter;
import static org.assertj.core.api.Assertions.*;
import static excelAssertions.UlpAssertions.assertThatDoubleIsCloseToInUlps;

class UlpAssertionsTest {

    @Test
    void exactMatchIsZeroUlps() {
        assertThatCode(() -> assertThatDoubleIsCloseToInUlps(1.0, 1.0, 0))
                .doesNotThrowAnyException();
    }

    @Test
    void oneUlpDifferenceSucceedsWithOneUlpAllowed() {
        double a = 1.0;
        double b = nextAfter(a, Double.POSITIVE_INFINITY);
        assertThatCode(() -> assertThatDoubleIsCloseToInUlps(a, b, 1))
                .doesNotThrowAnyException();
    }

    @Test
    void multipleUlpsWithinLimitPasses() {
        double a = 1.0;
        double b = a;
        for (int i = 0; i < 5; i++) b = nextAfter(b, Double.POSITIVE_INFINITY);

        final double fiveUlpsMore = b;

        assertThatCode(() -> assertThatDoubleIsCloseToInUlps(a, fiveUlpsMore, 5))
                .doesNotThrowAnyException();
    }

    @Test
    void ulpsExceedingLimitFails() {
        double a = 1.0;
        double b = a;
        for (int i = 0; i < 10; i++)
            b = nextAfter(b, Double.POSITIVE_INFINITY);
        final double tenUlpsMore = b;

        assertThatThrownBy(() -> assertThatDoubleIsCloseToInUlps(a, tenUlpsMore, 5))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("within <5> ULPs");
    }

    @Test
    void negativeNumbersWorkCorrectly() {
        double a = -1.0;
        double b = nextAfter(a, Double.NEGATIVE_INFINITY);
        assertThatCode(() -> assertThatDoubleIsCloseToInUlps(a, b, 1))
                .doesNotThrowAnyException();
    }

    @Test
    void zeroAndNextAfterZeroAreOneUlpApart() {
        double a = 0.0;
        double b = nextAfter(a, Double.POSITIVE_INFINITY);
        assertThatCode(() -> assertThatDoubleIsCloseToInUlps(a, b, 1))
                .doesNotThrowAnyException();
    }

    @Test
    void nanAsActualThrowsException() {
        assertThatThrownBy(() -> assertThatDoubleIsCloseToInUlps(Double.NaN, 1.0, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("NaN values are not supported");
    }

    @Test
    void nanAsExpectedThrowsException() {
        assertThatThrownBy(() -> assertThatDoubleIsCloseToInUlps(1.0, Double.NaN, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("NaN values are not supported");
    }

    @Test
    void bothPositiveInfinityAreEqualRegardlessOfUlps() {
        assertThatCode(() -> assertThatDoubleIsCloseToInUlps(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 0))
                .doesNotThrowAnyException();
    }

    @Test
    void positiveInfinityAndFiniteValueFails() {
        assertThatThrownBy(() -> assertThatDoubleIsCloseToInUlps(Double.POSITIVE_INFINITY, 1.0, 1000))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Expected <Infinity> to be close to");
    }

    @Test
    void bothNegativeInfinityAreEqualRegardlessOfUlps() {
        assertThatCode(() -> assertThatDoubleIsCloseToInUlps(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, 0))
                .doesNotThrowAnyException();
    }

    @Test
    void negativeInfinityAndFiniteValueFails() {
        assertThatThrownBy(() -> assertThatDoubleIsCloseToInUlps(Double.NEGATIVE_INFINITY, -1234.567, 9999))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Expected <-Infinity> to be close to");
    }

    @Test
    void positiveAndNegativeInfinityAreNotClose() {
        assertThatThrownBy(() -> assertThatDoubleIsCloseToInUlps(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 1))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Expected <Infinity> to be close to <-Infinity>, but one is infinite");

    }

    @Test
    void minValueAndZeroAreOneUlpApart() {
        double min = Double.MIN_VALUE; // Smallest positive non-zero double
        double zero = 0.0;
        assertThatCode(() -> assertThatDoubleIsCloseToInUlps(min, zero, 1))
                .doesNotThrowAnyException();
    }

    @Test
    void maxValueAndNextAfterTowardInfinityExceedsUlps() {
        double max = Double.MAX_VALUE;
        double next = nextAfter(max, Double.POSITIVE_INFINITY);
        assertThat(next).isEqualTo(Double.POSITIVE_INFINITY);

        // Expect failure if we don't allow for infinity
        assertThatThrownBy(() -> assertThatDoubleIsCloseToInUlps(max, next, 10))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("infinite");
    }

    @Test
    void valuesCloseToMaxValueWithinUlps() {
        double a = Double.MAX_VALUE;
        double b = nextAfter(a, Double.NEGATIVE_INFINITY); // slightly smaller
        assertThatCode(() -> assertThatDoubleIsCloseToInUlps(a, b, 1))
                .doesNotThrowAnyException();
    }
}
