package excelAssertions;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellReference;
import org.assertj.core.api.SoftAssertions;

public sealed abstract class CellAssertion<T extends CellAssertion<T>> permits NumberCellAssertion, StringCellAssertion /*FormulaCellAssertion, LocalDateTimeCellAssertion*/ {
    protected final String cellAddress;
    private String expectedFormat;

    protected CellAssertion(String cellAddress) {
        if (cellAddress == null) throw new IllegalArgumentException("cellAddress cannot be null");
        this.cellAddress = cellAddress;
    }

    public T format(String expectedFormat) {
        this.expectedFormat = expectedFormat;
        return self();
    }

    final void doAssert(Cell cell, SoftAssertions softly) {
        boolean mainAssertionAdded = false;
        if (expectedFormat != null) {
            var cellStyle = cell.getCellStyle();
            softly.assertThat(cellStyle)
                    .as("cellStyle for cell " + cellAddress + " does not exist")
                    .isNotNull();
            if (cellStyle != null) {
                String actualFormat = cellStyle.getDataFormatString();
                softly.assertThat(actualFormat)
                        .isEqualTo(expectedFormat);
            }
            mainAssertionAdded = true;
        }

        boolean additionalAssertionAdded = doAssertCore(cell, softly);

        if (!mainAssertionAdded && !additionalAssertionAdded)
            softly.fail("%s for %s does not add anything. What's the point of asserting?".formatted(this.getClass().getSimpleName(), cellAddress));
    }

    protected abstract boolean doAssertCore(Cell cell, SoftAssertions softly);


    @SuppressWarnings("unchecked")
    protected T self() {
        return (T) this;
    }

    Cell getCell(Sheet sheet) {
        CellReference cellReference = new CellReference(cellAddress);
        Row row = sheet.getRow(cellReference.getRow());
        if (row == null) return null;
        return row.getCell(cellReference.getCol());
    }
}