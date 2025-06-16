package excelAssertions;

import org.apache.poi.ss.usermodel.*;
import org.assertj.core.api.SoftAssertions;

abstract sealed class ErrorCellAssertion extends CellAssertion<String, ErrorCellAssertion>
        permits ErrorTextContainsCellAssertion, ErrorTextEqualsCellAssertion {
    protected ErrorCellAssertion(String cellAddress) {
        super(cellAddress);
    }

    @Override
    protected final boolean isCellTypeSupported(CellType cellType) {
        return cellType == CellType.ERROR;
    }

    @Override
    protected final String fromCell(Cell cell) {
        return FormulaError.forInt(cell.getErrorCellValue()).getString();
    }

    @Override
    protected final String fromCellValue(CellValue cellValue) {
        return FormulaError.forInt(cellValue.getErrorValue()).getString();
    }


    protected boolean ignoreCase = false;
    protected boolean ignoreNewLines = false;

    public ErrorCellAssertion ignoreCase() {
        this.ignoreCase = true;
        return self();
    }

    public ErrorCellAssertion ignoreNewLines() {
        this.ignoreNewLines = true;
        return self();
    }

    protected static String normalizeNewLines(String s) {
        return s.replaceAll("\\R", " ")  // replaces any newline with a space
                .replaceAll("\\s+", " ") // collapse multiple spaces
                .trim(); // optional: remove leading/trailing spaces
    }
}

final class ErrorTextEqualsCellAssertion extends ErrorCellAssertion {
    private final String expectedText;

    ErrorTextEqualsCellAssertion(String cellAddress, String equalsText) {
        super(cellAddress);
        this.expectedText = equalsText;
    }

    @Override
    protected void doAssertOnValue(final String actualValue, SoftAssertions softly) {
        String actual = ignoreNewLines ? normalizeNewLines(actualValue) : actualValue;
        String expected = ignoreNewLines ? normalizeNewLines(expectedText) : expectedText;

        if (ignoreCase)
            softly.assertThat(actual).isEqualToIgnoringCase(expected);
        else
            softly.assertThat(actual).isEqualTo(expected);
    }
}

final class ErrorTextContainsCellAssertion extends ErrorCellAssertion {
    private final String containsText;

    ErrorTextContainsCellAssertion(String cellAddress, String containsText) {
        super(cellAddress);
        this.containsText = containsText;
    }

    @Override
    protected void doAssertOnValue(final String actualValue, SoftAssertions softly) {
        String actual = ignoreNewLines ? normalizeNewLines(actualValue) : actualValue;
        String expected = ignoreNewLines ? normalizeNewLines(containsText) : containsText;

        if (ignoreCase)
            softly.assertThat(actual).containsIgnoringCase(expected);
        else
            softly.assertThat(actual).contains(expected);
    }
}
