package excelAssertions;

import org.apache.poi.ss.usermodel.*;
import org.assertj.core.api.SoftAssertions;
import org.jetbrains.annotations.NotNull;

@lombok.Getter(lombok.AccessLevel.PACKAGE)
@lombok.EqualsAndHashCode(callSuper = true)
public final class TextCellAssertion extends CellAssertion<String, TextCellAssertion> {
    private final @NotNull TextAssertion<?> assertion;

    public TextCellAssertion(String cellAddress, @NotNull TextAssertion<?> assertion) {
        super(cellAddress);
        this.assertion = assertion;
    }

    @Override
    protected void assertOnValue(String actualValue, SoftAssertions softly) {
        var softAssert = softly.assertThat(actualValue)
                .as(() -> "text at %s!%s to %s".formatted(getSheetName(), cellAddress, assertion.getFilterDescription()));
        assertion.apply(softAssert);
    }

    @Override
    protected boolean isCellTypeSupported(CellType cellType) {
        return cellType == CellType.STRING;
    }

    @Override
    protected String fromCell(Cell cell) {
        return cell.getStringCellValue();
    }

    @Override
    protected String fromCellValue(CellValue cellValue) {
        return cellValue.getStringValue();
    }
}