package excelAssertions;

import org.assertj.core.api.AbstractLongAssert;

public class UlpAssertions {

    public static void assertThatDoubleIsCloseToInUlps(double actual, double expected, long maxUlps) {
        if (Double.isNaN(actual) || Double.isNaN(expected)) {
            throw new IllegalArgumentException("NaN values are not supported");
        }

        if (Double.isInfinite(actual) || Double.isInfinite(expected)) {
            if (actual == expected) {
                return; // same infinities are always considered close
            } else {
                throw new AssertionError(String.format("Expected <%s> to be close to <%s>, but one is infinite", actual, expected));
            }
        }

        long actualBits = Double.doubleToLongBits(actual);
        long expectedBits = Double.doubleToLongBits(expected);
        long ulpDiff = Math.abs(actualBits - expectedBits);

        org.assertj.core.api.Assertions.assertThat(ulpDiff)
                .withFailMessage("Expected <%s> to be within <%d> ULPs of <%s> but difference was <%d> ULPs",
                        actual, maxUlps, expected, ulpDiff)
                .isLessThanOrEqualTo(maxUlps);
    }

    /*public static void assertThatDoubleIsCloseToInUlps(double actual, double expected, long maxUlps) {
        long actualBits = Double.doubleToLongBits(actual);
        long expectedBits = Double.doubleToLongBits(expected);
        long ulpDiff = Math.abs(actualBits - expectedBits);

        assertUlpsClose(org.assertj.core.api.Assertions.assertThat(ulpDiff), actual, expected, maxUlps);
    }

    public static void assertThatDoubleIsCloseToInUlps(org.assertj.core.api.SoftAssertions softly,
                                                       double actual, double expected, long maxUlps    ) {
        long actualBits = Double.doubleToLongBits(actual);
        long expectedBits = Double.doubleToLongBits(expected);
        long ulpDiff = Math.abs(actualBits - expectedBits);

        assertUlpsClose(softly.assertThat(ulpDiff), actual, expected, maxUlps);
    }


    private static void assertUlpsClose(
            AbstractLongAssert<?> assertion,
            double actual, double expected,
            long maxUlps
    ) {
        if (Double.isNaN(actual) || Double.isNaN(expected)) {
            assertion.withFailMessage("Cannot compare NaN values: actual=<%s>, expected=<%s>", actual, expected);
            return;
        }

        if (Double.isInfinite(actual) || Double.isInfinite(expected)) {
            if (actual == expected) return;
            assertion.withFailMessage("Infinities do not match: actual=<%s>, expected=<%s>", actual, expected);
            return;
        }

        long actualBits = Double.doubleToLongBits(actual);
        long expectedBits = Double.doubleToLongBits(expected);
        long ulpDiff = Math.abs(actualBits - expectedBits);

        assertion
                .withFailMessage("Expected <%s> to be within <%d> ULPs of <%s>, but difference was <%d> ULPs",
                        actual, maxUlps, expected, ulpDiff)
                .isLessThanOrEqualTo(maxUlps);
    }*/


}
