package excelAssertions;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.poi.ss.usermodel.*;
import org.assertj.core.api.SoftAssertions;
import org.jetbrains.annotations.NotNull;

@Getter(AccessLevel.PACKAGE)
@EqualsAndHashCode(callSuper = true)
public final class ErrorTextCellAssertion extends CellAssertion<String, ErrorTextCellAssertion> {
    private final @NotNull TextAssertion<?> assertion;

    public ErrorTextCellAssertion(String cellAddress, @NotNull TextAssertion<?> assertion) {
        super(cellAddress);
        this.assertion = assertion;
    }

    @Override
    protected void doAssertOnValue(String actualValue, SoftAssertions softly) {
        String operation = switch (assertion) {
            case ContainsTextAssertion ignored -> "contains";
            case EqualsTextAssertion ignored -> "equality";
            case PatternTextAssertion ignored -> "pattern match";
        };

        var softAssert = softly.assertThat(actualValue)
                .as(() -> "error text %s check at %s".formatted(operation, cellAddress));
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