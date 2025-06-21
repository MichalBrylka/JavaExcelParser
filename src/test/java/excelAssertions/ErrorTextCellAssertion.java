package excelAssertions;

import org.apache.poi.ss.usermodel.*;
import org.assertj.core.api.SoftAssertions;
import org.jetbrains.annotations.NotNull;

@lombok.Getter(lombok.AccessLevel.PACKAGE)
@lombok.EqualsAndHashCode(callSuper = true)
public final class ErrorTextCellAssertion extends CellAssertion<String, ErrorTextCellAssertion> {
    private final @NotNull TextAssertion<?> assertion;

    public ErrorTextCellAssertion(String cellAddress, @NotNull TextAssertion<?> assertion) {
        super(cellAddress);
        this.assertion = assertion;
    }

    @Override
    protected void assertOnValue(String actualValue, SoftAssertions softly) {
        var softAssert = softly.assertThat(actualValue)
                .as(() -> "error text at %s!%s to %s".formatted(getSheetName(), cellAddress, assertion.getFilterDescription()));
        assertion.apply(softAssert);
    }

    @Override
    protected boolean isCellTypeSupported(CellType cellType) {
        return cellType == CellType.ERROR;
    }

    @Override
    protected String fromCell(Cell cell) {
        return FormulaError.forInt(cell.getErrorCellValue()).getString();
    }

    @Override
    protected String fromCellValue(CellValue cellValue) {
        return FormulaError.forInt(cellValue.getErrorValue()).getString();
    }
}