package excelAssertions;

import org.apache.poi.ss.usermodel.*;
import org.assertj.core.api.SoftAssertions;


public final class StringCellAssertion extends CellAssertion<StringCellAssertion> {
    private String expectedValue;
    private String expectedContainsValue;
    private boolean ignoreCase = false;

    public StringCellAssertion(String cellAddress) {
        super(cellAddress);
    }

    public StringCellAssertion equalTo(String value) {
        this.expectedValue = value;
        this.ignoreCase = false;
        this.expectedContainsValue = null;
        return self();
    }

    public StringCellAssertion equalsIgnoreCase(String value) {
        this.expectedValue = value;
        this.ignoreCase = true;
        this.expectedContainsValue = null;
        return self();
    }

    public StringCellAssertion contains(String value) {
        this.expectedValue = null;
        this.ignoreCase = false;
        this.expectedContainsValue = value;
        return self();
    }

    public StringCellAssertion containsIgnoreCase(String value) {
        this.expectedValue = null;
        this.ignoreCase = true;
        this.expectedContainsValue = value;
        return self();
    }

    @Override
    protected boolean doAssertCore(Cell cell, SoftAssertions softly) {
        if (cell == null || cell.getCellType() != CellType.STRING) {
            softly.assertThat(cell)
                    .as("Cell " + cellAddress + " is not a string cell.")
                    .isNotNull()
                    .extracting(Cell::getCellType)
                    .isEqualTo(CellType.NUMERIC);
            return true;
        }

        String actual = cell.getStringCellValue();

        if (expectedValue != null) {
            if (ignoreCase)
                softly.assertThat(actual).isEqualTo(expectedValue);
            else
                softly.assertThat(actual).isEqualToIgnoringCase(expectedValue);

            return true;
        } else if (expectedContainsValue != null) {
            if (ignoreCase)
                softly.assertThat(actual).contains(expectedContainsValue);
            else
                softly.assertThat(actual).containsIgnoringCase(expectedContainsValue);

            return true;
        } else
            return false;

    }
}
