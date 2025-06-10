package excelAssertions;

import org.apache.poi.ss.usermodel.*;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.data.Offset;
import org.assertj.core.data.Percentage;

import org.assertj.core.api.Assertions;

public final class NumberCellAssertion extends CellAssertion<NumberCellAssertion> {
    private Double expectedValue;
    private Offset<Double> offset;
    private Percentage percentage;


    public NumberCellAssertion(String cellAddress) {
        super(cellAddress);
    }

    public NumberCellAssertion equalTo(double expectedValue) {
        this.expectedValue = expectedValue;
        return self();
    }

    public NumberCellAssertion withinOffset(double value) {
        this.offset = Assertions.offset(value);
        this.percentage = null;
        return self();
    }

    public NumberCellAssertion withinPercentage(double value) {
        this.offset = null;
        this.percentage = Assertions.withinPercentage(value);
        return self();
    }

    @Override
    public String toString() {
        return
                "expectedValue==" + expectedValue +
                (
                        offset != null || percentage != null
                                ? (
                                offset != null
                                        ? " ±" + offset.value
                                        : " ±" + percentage
                        )
                                : ""

                );
    }

    @Override
    protected boolean doAssertCore(Cell cell, SoftAssertions softly) {
        if (cell == null || cell.getCellType() != CellType.NUMERIC) {
            softly.assertThat(cell)
                    .as("Cell " + cellAddress + " is not a numeric cell.")
                    .isNotNull()
                    .extracting(Cell::getCellType)
                    .isEqualTo(CellType.NUMERIC);
            return true;
        }

        double actualValue = cell.getNumericCellValue();

        if (expectedValue instanceof Double d) {
            double expected = d;

            if (offset == null && percentage != null)
                softly.assertThat(actualValue).isCloseTo(expected, percentage);
            else if (offset != null && percentage == null)
                softly.assertThat(actualValue).isCloseTo(expected, offset);
            else
                softly.assertThat(actualValue).isEqualTo(expected);

            return true;
        } else
            return false;
    }
}