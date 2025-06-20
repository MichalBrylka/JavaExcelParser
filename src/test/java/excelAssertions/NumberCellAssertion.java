package excelAssertions;

import org.apache.poi.ss.usermodel.*;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.data.Offset;
import org.assertj.core.data.Percentage;

@lombok.Getter(lombok.AccessLevel.PACKAGE)
@lombok.EqualsAndHashCode(callSuper = true)
sealed abstract class NumberCellAssertion extends CellAssertion<Double, NumberCellAssertion> permits
        CloseToOffsetNumberCellAssertion, CloseToPercentNumberCellAssertion, RelationNumberCellAssertion {

    protected NumberCellAssertion(String cellAddress) {
        super(cellAddress);
    }

    @Override
    protected final boolean isCellTypeSupported(CellType cellType) {
        return cellType == CellType.NUMERIC;
    }

    @Override
    protected final Double fromCell(Cell cell) {
        return cell.getNumericCellValue();
    }

    @Override
    protected final Double fromCellValue(CellValue cellValue) {
        return cellValue.getNumberValue();
    }

    protected String getErrorDetails() {
        return "number check at " + cellAddress;
    }
}

@lombok.Getter(lombok.AccessLevel.PACKAGE)
@lombok.EqualsAndHashCode(callSuper = true)
final class CloseToPercentNumberCellAssertion extends NumberCellAssertion {
    private final Double expectedValue;
    private final Percentage percentage;

    CloseToPercentNumberCellAssertion(String cellAddress, Double expectedValue, Percentage percentage) {
        super(cellAddress);
        this.expectedValue = expectedValue;
        this.percentage = percentage;
    }

    @Override
    protected void assertOnValue(Double actualValue, SoftAssertions softly) {
        softly.assertThat(actualValue)
                .as(this::getErrorDetails)
                .isCloseTo(expectedValue, percentage);
    }
}

@lombok.Getter(lombok.AccessLevel.PACKAGE)
@lombok.EqualsAndHashCode(callSuper = true)
final class CloseToOffsetNumberCellAssertion extends NumberCellAssertion {
    private final Double expectedValue;
    private final Offset<Double> offset;

    CloseToOffsetNumberCellAssertion(String cellAddress, Double expectedValue, Offset<Double> offset) {
        super(cellAddress);
        this.expectedValue = expectedValue;
        this.offset = offset;
    }

    @Override
    protected void assertOnValue(Double actualValue, SoftAssertions softly) {
        softly.assertThat(actualValue)
                .as(this::getErrorDetails)
                .isCloseTo(expectedValue, offset);
    }
}

@lombok.Getter(lombok.AccessLevel.PACKAGE)
@lombok.EqualsAndHashCode(callSuper = true)
sealed abstract class RelationNumberCellAssertion extends NumberCellAssertion permits
        EqualToNumberCellAssertion,
        GreaterThanNumberCellAssertion, GreaterThanOrEqualToNumberCellAssertion,
        LessThanNumberCellAssertion, LessThanOrEqualToNumberCellAssertion {

    protected final Double expectedValue;

    protected RelationNumberCellAssertion(String cellAddress, Double expectedValue) {
        super(cellAddress);
        this.expectedValue = expectedValue;
    }
}

@lombok.Getter(lombok.AccessLevel.PACKAGE)
@lombok.EqualsAndHashCode(callSuper = true)
final class EqualToNumberCellAssertion extends RelationNumberCellAssertion {

    EqualToNumberCellAssertion(String cellAddress, Double expectedValue) {
        super(cellAddress, expectedValue);
    }

    @Override
    protected void assertOnValue(Double actualValue, SoftAssertions softly) {
        softly.assertThat(actualValue)
                .as(this::getErrorDetails)
                .isEqualTo(expectedValue);
    }
}

@lombok.Getter(lombok.AccessLevel.PACKAGE)
@lombok.EqualsAndHashCode(callSuper = true)
final class GreaterThanNumberCellAssertion extends RelationNumberCellAssertion {

    GreaterThanNumberCellAssertion(String cellAddress, Double expectedValue) {
        super(cellAddress, expectedValue);
    }

    @Override
    protected void assertOnValue(Double actualValue, SoftAssertions softly) {
        softly.assertThat(actualValue)
                .as(this::getErrorDetails)
                .isGreaterThan(expectedValue);
    }
}

@lombok.Getter(lombok.AccessLevel.PACKAGE)
@lombok.EqualsAndHashCode(callSuper = true)
final class GreaterThanOrEqualToNumberCellAssertion extends RelationNumberCellAssertion {

    GreaterThanOrEqualToNumberCellAssertion(String cellAddress, Double expectedValue) {
        super(cellAddress, expectedValue);
    }

    @Override
    protected void assertOnValue(Double actualValue, SoftAssertions softly) {
        softly.assertThat(actualValue)
                .as(this::getErrorDetails)
                .isGreaterThanOrEqualTo(expectedValue);
    }
}

@lombok.Getter(lombok.AccessLevel.PACKAGE)
@lombok.EqualsAndHashCode(callSuper = true)
final class LessThanNumberCellAssertion extends RelationNumberCellAssertion {

    LessThanNumberCellAssertion(String cellAddress, Double expectedValue) {
        super(cellAddress, expectedValue);
    }

    @Override
    protected void assertOnValue(Double actualValue, SoftAssertions softly) {
        softly.assertThat(actualValue)
                .as(this::getErrorDetails)
                .isLessThan(expectedValue);
    }
}

@lombok.Getter(lombok.AccessLevel.PACKAGE)
@lombok.EqualsAndHashCode(callSuper = true)
final class LessThanOrEqualToNumberCellAssertion extends RelationNumberCellAssertion {

    LessThanOrEqualToNumberCellAssertion(String cellAddress, Double expectedValue) {
        super(cellAddress, expectedValue);
    }

    @Override
    protected void assertOnValue(Double actualValue, SoftAssertions softly) {
        softly.assertThat(actualValue)
                .as(this::getErrorDetails)
                .isLessThanOrEqualTo(expectedValue);
    }
}
