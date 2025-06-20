package excelAssertions;

import org.apache.poi.ss.usermodel.*;
import org.assertj.core.api.SoftAssertions;

@lombok.Getter(lombok.AccessLevel.PACKAGE)
@lombok.EqualsAndHashCode(callSuper = true)
public final class BooleanCellAssertion extends CellAssertion<Boolean, BooleanCellAssertion> {
    private final boolean expectedValue;

    public BooleanCellAssertion(String cellAddress, boolean expectedValue) {
        super(cellAddress);
        this.expectedValue = expectedValue;
    }

    @Override
    protected void assertOnValue(Boolean actualValue, SoftAssertions softly) {
        softly.assertThat(actualValue)
                .as(() -> "boolean check at " + cellAddress)
                .isEqualTo(expectedValue);
    }

    @Override
    protected boolean isCellTypeSupported(CellType cellType) {
        return cellType == CellType.BOOLEAN;
    }

    @Override
    protected Boolean fromCell(Cell cell) {
        return cell.getBooleanCellValue();
    }

    @Override
    protected Boolean fromCellValue(CellValue cellValue) {
        return cellValue.getBooleanValue();
    }
}
