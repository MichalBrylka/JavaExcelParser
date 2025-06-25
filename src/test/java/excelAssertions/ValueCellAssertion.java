package excelAssertions;

import org.apache.poi.ss.usermodel.*;
import org.assertj.core.api.SoftAssertions;

@lombok.Getter(lombok.AccessLevel.PACKAGE)
@lombok.EqualsAndHashCode(callSuper = false)
public sealed abstract class ValueCellAssertion<TValue, TAssertion extends ValueCellAssertion<TValue, TAssertion>> extends CellAssertion<ValueCellAssertion<TValue, TAssertion>>
        permits BooleanCellAssertion, ErrorTextCellAssertion, FormulaTextCellAssertion, NumberCellAssertion, TextCellAssertion {

    protected ValueCellAssertion(String cellAddress) {
        super(cellAddress);
    }

    @Override
    protected final void applyAssertCore(Cell cell, SoftAssertions softly) {
        if (cell != null) {
            CellType cellType = cell.getCellType();
            if (isCellTypeSupported(cellType)) {
                assertOnValue(fromCell(cell), softly);
                return;
            } else if (CellType.FORMULA == cellType) {
                CellValue cellValue = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator().evaluate(cell);
                CellType cellValueType = cellValue.getCellType();

                if (isCellTypeSupported(cellValueType))
                    assertOnValue(fromCellValue(cellValue), softly);
                else
                    softly.fail("%s: cannot add assertion for formula cell %s %s: '%s'".formatted(this.getClass().getSimpleName(), getFullCellAddress(), cellValueType, cell.getStringCellValue()));
                return;
            }
        }
        softly.fail("%s: cannot add assertion for cell %s:'%s'".formatted(this.getClass().getSimpleName(), getFullCellAddress(), cell == null ? "<EMPTY>" : cell.getStringCellValue()));
    }

    protected abstract void assertOnValue(TValue value, SoftAssertions softly);

    protected abstract boolean isCellTypeSupported(CellType cellType);

    protected abstract TValue fromCell(Cell cell);

    protected abstract TValue fromCellValue(CellValue cellValue);
}