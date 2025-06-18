package excelAssertions;

import org.apache.poi.ss.usermodel.*;
import org.assertj.core.api.SoftAssertions;

@lombok.EqualsAndHashCode(callSuper = true)
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


    protected boolean isIgnoreCase = false;
    protected boolean isIgnoreNewLines = false;

    public ErrorCellAssertion ignoreCase() {
        this.isIgnoreCase = true;
        return self();
    }

    public ErrorCellAssertion ignoreNewLines() {
        this.isIgnoreNewLines = true;
        return self();
    }

    protected static String normalizeNewLines(String s) {
        return s.replaceAll("\\R", " ")  // replaces any newline with a space
                .replaceAll("\\s+", " ") // collapse multiple spaces
                .trim(); // optional: remove leading/trailing spaces
    }
}

@lombok.Getter(lombok.AccessLevel.PACKAGE)
@lombok.EqualsAndHashCode(callSuper = true)
final class ErrorTextEqualsCellAssertion extends ErrorCellAssertion {
    private final String expectedText;

    ErrorTextEqualsCellAssertion(String cellAddress, String equalsText) {
        super(cellAddress);
        this.expectedText = equalsText;
    }

    @Override
    protected void doAssertOnValue(final String actualValue, SoftAssertions softly) {
        String actual = isIgnoreNewLines ? normalizeNewLines(actualValue) : actualValue;
        String expected = isIgnoreNewLines ? normalizeNewLines(expectedText) : expectedText;

        if (isIgnoreCase)
            softly.assertThat(actual).isEqualToIgnoringCase(expected);
        else
            softly.assertThat(actual).isEqualTo(expected);
    }
}

@lombok.Getter(lombok.AccessLevel.PACKAGE)
@lombok.EqualsAndHashCode(callSuper = true)
final class ErrorTextContainsCellAssertion extends ErrorCellAssertion {
    private final String containsText;

    ErrorTextContainsCellAssertion(String cellAddress, String containsText) {
        super(cellAddress);
        this.containsText = containsText;
    }

    @Override
    protected void doAssertOnValue(final String actualValue, SoftAssertions softly) {
        String actual = isIgnoreNewLines ? normalizeNewLines(actualValue) : actualValue;
        String expected = isIgnoreNewLines ? normalizeNewLines(containsText) : containsText;

        if (isIgnoreCase)
            softly.assertThat(actual).containsIgnoringCase(expected);
        else
            softly.assertThat(actual).contains(expected);
    }
}
