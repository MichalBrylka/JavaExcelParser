package excelAssertions;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import org.assertj.core.api.SoftAssertions;

public sealed abstract class CellAssertion<TValue, TAssertion extends CellAssertion<TValue, TAssertion>> permits NumberCellAssertion {
    protected final String cellAddress;
    private String expectedFormat;

    protected CellAssertion(String cellAddress) {
        if (cellAddress == null || cellAddress.isBlank())
            throw new IllegalArgumentException("cellAddress cannot be null nor blank");
        this.cellAddress = cellAddress;
    }

    public TAssertion withFormat(String expectedFormat) {
        this.expectedFormat = expectedFormat;
        return self();
    }

    @SuppressWarnings("unchecked")
    protected TAssertion self() {
        return (TAssertion) this;
    }


    final void doAssert(Cell cell, SoftAssertions softly) {
        if (expectedFormat != null) {
            softly.assertThat(cell.getCellStyle())
                    .withFailMessage(() -> "cellStyle for cell " + cellAddress + " does not exist")
                    .isNotNull()
                    .extracting(CellStyle::getDataFormatString)
                    .isEqualTo(expectedFormat);
        }

        doAssertCore(cell, softly);
    }

    protected void doAssertCore(Cell cell, SoftAssertions softly) {
        CellType cellType = cell.getCellType();
        if (isCellTypeSupported(cellType))
            doAssertOnValue(fromCell(cell), softly);
        else if (CellType.FORMULA != cellType) {
            CellValue cellValue = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator().evaluate(cell);
            CellType cellValueType = cellValue.getCellType();

            if (isCellTypeSupported(cellValueType))
                doAssertOnValue(fromCellValue(cellValue), softly);
            else
                softly.fail("%s for %s cannot add assertion for formula cell type %s: '%s'".formatted(this.getClass().getSimpleName(), cellAddress, cellValueType, cell.getStringCellValue()));
        } else
            softly.fail("%s for %s cannot add assertion for cell type %s: '%s'".formatted(this.getClass().getSimpleName(), cellAddress, cellType, cell.getStringCellValue()));
    }

    protected abstract void doAssertOnValue(TValue value, SoftAssertions softly);

    protected abstract boolean isCellTypeSupported(CellType cellType);

    protected abstract TValue fromCell(Cell cell);

    protected abstract TValue fromCellValue(CellValue cellValue);

    Cell getCell(Sheet sheet) {
        CellReference cellReference = new CellReference(cellAddress);
        Row row = sheet.getRow(cellReference.getRow());
        if (row == null) return null;
        return row.getCell(cellReference.getCol());
    }
}