package excelAssertions;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import org.assertj.core.api.SoftAssertions;

@lombok.Getter(lombok.AccessLevel.PACKAGE)
@lombok.EqualsAndHashCode(callSuper = false)
public sealed abstract class CellAssertion<TValue, TAssertion extends CellAssertion<TValue, TAssertion>> permits BooleanCellAssertion, EmptyCellAssertion, ErrorTextCellAssertion, FormulaTextCellAssertion, NumberCellAssertion, TextCellAssertion {

    protected final String cellAddress;
    protected TextAssertion<?> expectedFormat;
    protected FormatCategory expectedFormatCategory;

    protected CellAssertion(String cellAddress) {
        if (cellAddress == null || cellAddress.isBlank())
            throw new IllegalArgumentException("cellAddress cannot be null nor blank");
        this.cellAddress = cellAddress;
    }

    public TAssertion withFormat(TextAssertion<?> expectedFormat) {
        this.expectedFormat = expectedFormat;
        return self();
    }

    public TAssertion withFormat(String expectedFormat) {
        this.expectedFormat = new EqualsTextAssertion(expectedFormat, false, false);
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
            var softAssert = softly.assertThat(getCellFormat(cell))
                    .as(() -> "cell format at %s to %s".formatted(cellAddress, expectedFormat.getFilterDescription()));
            expectedFormat.apply(softAssert);
        }
        if (expectedFormatCategory != null) {
            var actual = detectFormatCategory(cell);
            softly.assertThat(actual)
                    .as(() -> "expected format category at " + cellAddress)
                    .isEqualTo(expectedFormatCategory);
        }

        doAssertCore(cell, softly);
    }

    protected void doAssertCore(Cell cell, SoftAssertions softly) {
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
                    softly.fail("%s: cannot add assertion for formula cell @%s %s: '%s'".formatted(this.getClass().getSimpleName(), cellAddress, cellValueType, cell.getStringCellValue()));
                return;
            }
        }
        softly.fail("%s: cannot add assertion for cell @%s:'%s'".formatted(this.getClass().getSimpleName(), cellAddress, cell == null ? "<EMPTY>" : cell.getStringCellValue()));
    }

    protected abstract void assertOnValue(TValue value, SoftAssertions softly);

    protected abstract boolean isCellTypeSupported(CellType cellType);

    protected abstract TValue fromCell(Cell cell);

    protected abstract TValue fromCellValue(CellValue cellValue);

    private static String getCellFormat(Cell cell) {
        return cell != null && cell.getCellStyle() instanceof CellStyle style && style.getDataFormatString() instanceof String format
                ? format
                : null;
    }

    private static FormatCategory detectFormatCategory(Cell cell) {
        if (getCellFormat(cell) instanceof String format) {
            format = format.toLowerCase(java.util.Locale.ROOT);

            if (format.equals("general")) return FormatCategory.GENERAL;
            if (format.contains("%")) return FormatCategory.PERCENTAGE;
            if (DateUtil.isADateFormat(cell.getCellStyle().getDataFormat(), format)) return FormatCategory.DATE;
            if (format.contains("h") || format.contains("s") || format.contains("am/pm")) return FormatCategory.TIME;
            if (format.contains("#,##0") || format.contains("currency")) return FormatCategory.CURRENCY;
            if (format.contains("_($") || format.contains("accounting")) return FormatCategory.ACCOUNTING;
            if (format.contains("e+")) return FormatCategory.SCIENTIFIC;
            if (format.contains("?/")) return FormatCategory.FRACTION;
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