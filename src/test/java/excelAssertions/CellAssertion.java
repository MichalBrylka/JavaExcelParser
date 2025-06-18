package excelAssertions;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import org.assertj.core.api.SoftAssertions;

@lombok.Getter(lombok.AccessLevel.PACKAGE)
@lombok.EqualsAndHashCode(callSuper = false)
public sealed abstract class CellAssertion<TValue, TAssertion extends CellAssertion<TValue, TAssertion>> permits BooleanCellAssertion, EmptyCellAssertion, ErrorCellAssertion, NumberCellAssertion {

    protected final String cellAddress;
    protected String expectedFormat;
    protected FormatCategory expectedFormatCategory;

    protected CellAssertion(String cellAddress) {
        if (cellAddress == null || cellAddress.isBlank())
            throw new IllegalArgumentException("cellAddress cannot be null nor blank");
        this.cellAddress = cellAddress;
    }

    public TAssertion withFormat(String expectedFormat) {
        this.expectedFormat = expectedFormat;
        return self();
    }

    public TAssertion withFormatCategory(FormatCategory expectedFormatCategory) {
        this.expectedFormatCategory = expectedFormatCategory;
        return self();
    }

    @SuppressWarnings("unchecked")
    protected TAssertion self() {
        return (TAssertion) this;
    }


    final void doAssert(Cell cell, SoftAssertions softly) {

        if (expectedFormat != null) {
            softly.assertThat(cell)
                    .withFailMessage(() -> "cell for " + cellAddress + " cannot be null and it's cellStyle must exist for format assertion to work")
                    .isNotNull()

                    .extracting(Cell::getCellStyle)
                    .isNotNull()

                    .extracting(CellStyle::getDataFormatString)
                    .isEqualTo(expectedFormat);
        }
        if (expectedFormatCategory != null) {
            var actual = detectFormatCategory(cell);
            softly.assertThat(actual).isEqualTo(expectedFormatCategory);
        }

        doAssertCore(cell, softly);
    }

    protected void doAssertCore(Cell cell, SoftAssertions softly) {
        CellType cellType = cell.getCellType();
        if (isCellTypeSupported(cellType))
            doAssertOnValue(fromCell(cell), softly);
        else if (CellType.FORMULA == cellType) {
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

    protected static FormatCategory detectFormatCategory(Cell cell) {
        if (cell != null && cell.getCellStyle() instanceof CellStyle style && style.getDataFormatString() instanceof String format) {
            format = format.toLowerCase(java.util.Locale.ROOT);

            if (format.equals("general")) return FormatCategory.GENERAL;
            if (format.contains("%")) return FormatCategory.PERCENTAGE;
            if (format.contains("_($") || format.contains("accounting")) return FormatCategory.ACCOUNTING;
            if (DateUtil.isADateFormat(style.getDataFormat(), format)) return FormatCategory.DATE;
            if (format.contains("h") || format.contains("s") || format.contains("am/pm")) return FormatCategory.TIME;
            if (format.contains("#,##0") || format.contains("currency")) return FormatCategory.CURRENCY;
            if (format.contains("@")) return FormatCategory.TEXT;
        }
        return FormatCategory.OTHER;
    }

    Cell getCell(Sheet sheet) {
        CellReference cellReference = new CellReference(cellAddress);
        Row row = sheet.getRow(cellReference.getRow());
        if (row == null) return null;
        return row.getCell(cellReference.getCol());
    }
}