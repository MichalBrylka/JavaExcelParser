package excelAssertions;

import org.apache.poi.ss.usermodel.*;
import org.assertj.core.api.SoftAssertions;
import org.jetbrains.annotations.NotNull;

@lombok.Getter(lombok.AccessLevel.PACKAGE)
@lombok.EqualsAndHashCode(callSuper = true)
public final class FormulaTextCellAssertion extends CellAssertion<String, FormulaTextCellAssertion> {
    private final @NotNull TextAssertion<?> assertion;

    public FormulaTextCellAssertion(String cellAddress, @NotNull TextAssertion<?> assertion) {
        super(cellAddress);
        this.assertion = assertion;
    }

    @Override
    protected void assertOnValue(String actualValue, SoftAssertions softly) {
        var softAssert = softly.assertThat(actualValue)
                .as(() -> "formula text at %s to %s".formatted(cellAddress, assertion.getFilterDescription()));
        assertion.apply(softAssert);
    }

    @Override
    protected boolean isCellTypeSupported(CellType cellType) {
        return cellType == CellType.FORMULA;
    }

    @Override
    protected String fromCell(Cell cell) {
        return cell.getCellFormula();
    }

    @Override
    protected String fromCellValue(CellValue cellValue) {
        return cellValue.getStringValue();
    }
}