package excelAssertions;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;

/*
public final class LocalDateTimeCellAssertion extends CellAssertion<LocalDateTimeCellAssertion> {

    private LocalDateTime expectedValue;
    private TemporalAmount tolerance;

    public LocalDateTimeCellAssertion(String cellAddress) {
        super(cellAddress);
    }

    public LocalDateTimeCellAssertion equalTo(LocalDateTime expectedValue) {
        this.expectedValue = expectedValue;
        return self();
    }

    public LocalDateTimeCellAssertion within(TemporalAmount tolerance) {
        this.tolerance = tolerance;
        return self();
    }

    @Override
    public void doAssert(Workbook workbook) {
        Cell cell = getCell(workbook, cellAddress);

        if (cell == null || !DateUtil.isCellDateFormatted(cell)) {
            throw new AssertionError(String.format("Cell '%s' is not a valid date/time cell.", cellAddress));
        }

        LocalDateTime actualValue = cell.getLocalDateTimeCellValue();

        if (expectedValue != null) {
            if (tolerance != null) {
                long diff = ChronoUnit.NANOS.between(expectedValue, actualValue);
                long toleranceNanos = tolerance.get(ChronoUnit.SECONDS) * 1_000_000_000L + tolerance.get(ChronoUnit.NANOS);
                if (Math.abs(diff) > toleranceNanos) {
                    throw new AssertionError(String.format(
                            "Cell '%s' value <%s> is not within <%s> of <%s>",
                            cellAddress, actualValue, tolerance, expectedValue
                    ));
                }
            } else {
                if (!expectedValue.equals(actualValue)) {
                    throw new AssertionError(String.format(
                            "Cell '%s' value <%s> is not equal to expected <%s>",
                            cellAddress, actualValue, expectedValue
                    ));
                }
            }
        }
    }
}*/
