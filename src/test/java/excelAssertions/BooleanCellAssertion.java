package excelAssertions;

import org.apache.poi.ss.usermodel.*;
import org.assertj.core.api.SoftAssertions;

final class BooleanCellAssertion extends CellAssertion<Boolean, BooleanCellAssertion> {
    private final boolean expectedValue;

    public BooleanCellAssertion(String cellAddress, boolean expectedValue) {
        super(cellAddress);
        this.expectedValue = expectedValue;
    }

    @Override
    protected void doAssertOnValue(Boolean actualValue, SoftAssertions softly) {
        softly.assertThat(actualValue).isEqualTo(expectedValue);
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
