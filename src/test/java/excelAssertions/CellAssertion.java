package excelAssertions;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import org.assertj.core.api.SoftAssertions;

@lombok.Getter(lombok.AccessLevel.PACKAGE)
@lombok.EqualsAndHashCode(callSuper = false)
public sealed abstract class CellAssertion<TAssertion extends CellAssertion<TAssertion>>
        permits SimpleCellAssertion, EmptyCellAssertion, ValueCellAssertion {

    protected final String cellAddress;
    protected TextAssertion<?> expectedFormat;
    protected FormatCategory expectedFormatCategory;
    protected TextAssertion<?> expectedComment;

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

    public TAssertion withComment(TextAssertion<?> expectedComment) {
        this.expectedComment = expectedComment;
        return self();
    }

    public TAssertion withComment(String expectedComment) {
        this.expectedComment = new EqualsTextAssertion(expectedComment, false, false);
        return self();
    }

    public TAssertion withFormatCategory(FormatCategory expectedFormatCategory) {
        this.expectedFormatCategory = expectedFormatCategory;
        return self();
    }

    private String sheetName;

    String getSheetName() {
        return sheetName;
    }

    TAssertion withSheetName(String sheetName) {
        this.sheetName = sheetName;
        return self();
    }


    @SuppressWarnings("unchecked")
    protected TAssertion self() {
        return (TAssertion) this;
    }


    final void applyAssert(Cell cell, SoftAssertions softly) {
        if (expectedFormat != null) {
            var softAssert = softly.assertThat(getCellFormat(cell))
                    .as(() -> "cell format at %s!%s to %s".formatted(sheetName, cellAddress, expectedFormat.toString()));
            expectedFormat.apply(softAssert);
        }
        if (expectedFormatCategory != null) {
            var actual = detectFormatCategory(cell);
            softly.assertThat(actual)
                    .as(() -> "expected format category at %s!%s".formatted(sheetName, cellAddress))
                    .isEqualTo(expectedFormatCategory);
        }

        if (expectedComment != null) {
            var softAssert = softly.assertThat(getCellComment(cell))
                    .as(() -> "cell comment at %s!%s to %s".formatted(sheetName, cellAddress, expectedComment.toString()));
            expectedComment.apply(softAssert);
        }

        applyAssertCore(cell, softly);
    }

    protected abstract void applyAssertCore(Cell cell, SoftAssertions softly);

    private static String getCellFormat(Cell cell) {
        return cell != null && cell.getCellStyle() instanceof CellStyle style && style.getDataFormatString() instanceof String format
                ? format
                : null;
    }

    private static String getCellComment(Cell cell) {
        return cell != null && cell.getCellComment() instanceof Comment comment &&
               comment.getString() instanceof RichTextString richTextString &&
               richTextString.getString() instanceof String commentText
                ? commentText
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