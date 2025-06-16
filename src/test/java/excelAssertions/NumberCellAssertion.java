package excelAssertions;

import org.apache.poi.ss.usermodel.*;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.data.Offset;
import org.assertj.core.data.Percentage;

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
}

final class CloseToPercentNumberCellAssertion extends NumberCellAssertion {
    private final Double expectedValue;
    private final Percentage percentage;

    CloseToPercentNumberCellAssertion(String cellAddress, Double expectedValue, Percentage percentage) {
        super(cellAddress);
        this.expectedValue = expectedValue;
        this.percentage = percentage;
    }

    @Override
    protected void doAssertOnValue(Double actualValue, SoftAssertions softly) {
        softly.assertThat(actualValue).isCloseTo(expectedValue, percentage);
    }
}

final class CloseToOffsetNumberCellAssertion extends NumberCellAssertion {
    private final Double expectedValue;
    private final Offset<Double> offset;

    CloseToOffsetNumberCellAssertion(String cellAddress, Double expectedValue, Offset<Double> offset) {
        super(cellAddress);
        this.expectedValue = expectedValue;
        this.offset = offset;
    }

    @Override
    protected void doAssertOnValue(Double actualValue, SoftAssertions softly) {
        softly.assertThat(actualValue).isCloseTo(expectedValue, offset);
    }
}

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


final class EqualToNumberCellAssertion extends RelationNumberCellAssertion {

    EqualToNumberCellAssertion(String cellAddress, Double expectedValue) {
        super(cellAddress, expectedValue);
    }

    @Override
    protected void doAssertOnValue(Double actualValue, SoftAssertions softly) {
        softly.assertThat(actualValue).isEqualTo(expectedValue);
    }
}

final class GreaterThanNumberCellAssertion extends RelationNumberCellAssertion {

    GreaterThanNumberCellAssertion(String cellAddress, Double expectedValue) {
        super(cellAddress, expectedValue);
    }

    @Override
    protected void doAssertOnValue(Double actualValue, SoftAssertions softly) {
        softly.assertThat(actualValue).isGreaterThan(expectedValue);
    }
}

final class GreaterThanOrEqualToNumberCellAssertion extends RelationNumberCellAssertion {

    GreaterThanOrEqualToNumberCellAssertion(String cellAddress, Double expectedValue) {
        super(cellAddress, expectedValue);
    }

    @Override
    protected void doAssertOnValue(Double actualValue, SoftAssertions softly) {
        softly.assertThat(actualValue).isGreaterThanOrEqualTo(expectedValue);
    }
}

final class LessThanNumberCellAssertion extends RelationNumberCellAssertion {

    LessThanNumberCellAssertion(String cellAddress, Double expectedValue) {
        super(cellAddress, expectedValue);
    }

    @Override
    protected void doAssertOnValue(Double actualValue, SoftAssertions softly) {
        softly.assertThat(actualValue).isLessThan(expectedValue);
    }
}

final class LessThanOrEqualToNumberCellAssertion extends RelationNumberCellAssertion {

    LessThanOrEqualToNumberCellAssertion(String cellAddress, Double expectedValue) {
        super(cellAddress, expectedValue);
    }

    @Override
    protected void doAssertOnValue(Double actualValue, SoftAssertions softly) {
        softly.assertThat(actualValue).isLessThanOrEqualTo(expectedValue);
    }
}
