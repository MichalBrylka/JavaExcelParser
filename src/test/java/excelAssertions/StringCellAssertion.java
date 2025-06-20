package excelAssertions;

import org.apache.poi.ss.usermodel.*;
import org.assertj.core.api.SoftAssertions;
import org.jetbrains.annotations.NotNull;

@lombok.Getter(lombok.AccessLevel.PACKAGE)
@lombok.EqualsAndHashCode(callSuper = true)
public final class StringCellAssertion extends CellAssertion<String, StringCellAssertion> {
    private final @NotNull TextAssertion<?> assertion;

    public StringCellAssertion(String cellAddress, @NotNull TextAssertion<?> assertion) {
        super(cellAddress);
        this.assertion = assertion;
    }

    @Override
    protected void assertOnValue(String actualValue, SoftAssertions softly) {
        var softAssert = softly.assertThat(actualValue)
                .as(() -> "text %s check at %s".formatted(assertion.getFilterName(), cellAddress));
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