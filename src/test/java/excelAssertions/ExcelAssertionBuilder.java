package excelAssertions;

import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.assertj.core.data.Offset;
import org.assertj.core.data.Percentage;

import java.io.*;

public class ExcelAssertionBuilder {

    public static ExcelAssert assertThatExcel(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            return new ExcelAssert(WorkbookFactory.create(fis));
        } catch (IOException e) {
            // A failure to open the file should still fail fast.
            throw new AssertionError("Failed to read Excel file: <" + file.getName() + ">. Reason: " + e.getMessage(), e);
        }
    }

    public static ExcelAssert assertThatExcel(String filePath) {
        return assertThatExcel(new File(filePath));
    }

    public static ExcelAssert assertThatExcel(byte[] excelBytes) {
        try (InputStream is = new ByteArrayInputStream(excelBytes)) {
            return new ExcelAssert(WorkbookFactory.create(is));
        } catch (IOException e) {
            throw new AssertionError("Failed to read Excel bytes: <" + excelBytes.length + ">. Reason: " + e.getMessage(), e);
        }
    }


    private final String cellAddress;

    private ExcelAssertionBuilder(String cellAddress) {
        this.cellAddress = cellAddress;
    }

    public static ExcelAssertionBuilder cellAt(String cellAddress) {
        return new ExcelAssertionBuilder(cellAddress);
    }

    public NumberCellAssertionBuilder withNumber() {
        return new NumberCellAssertionBuilder(cellAddress);
    }

    public static class NumberCellAssertionBuilder {
        private final String cellAddress;

        public NumberCellAssertionBuilder(String cellAddress) {
            this.cellAddress = cellAddress;
        }

        public CellAssertion<Double, ?> isCloseTo(Double expectedValue, Percentage percentage) {
            return new CloseToPercentNumberCellAssertion(cellAddress, expectedValue, percentage);
        }

        public CellAssertion<Double, ?> isCloseTo(Double expectedValue, Offset<Double> offset) {
            return new CloseToOffsetNumberCellAssertion(cellAddress, expectedValue, offset);
        }

        public CellAssertion<Double, ?> equalTo(Double expectedValue) {
            return new EqualToNumberCellAssertion(cellAddress, expectedValue);
        }

        public CellAssertion<Double, ?> greaterThan(Double expectedValue) {
            return new GreaterThanNumberCellAssertion(cellAddress, expectedValue);
        }

        public CellAssertion<Double, ?> greaterThanOrEqualTo(Double expectedValue) {
            return new GreaterThanOrEqualToNumberCellAssertion(cellAddress, expectedValue);
        }

        public CellAssertion<Double, ?> lessThan(Double expectedValue) {
            return new LessThanNumberCellAssertion(cellAddress, expectedValue);
        }

        public CellAssertion<Double, ?> lessThanOrEqualTo(Double expectedValue) {
            return new LessThanOrEqualToNumberCellAssertion(cellAddress, expectedValue);
        }
    }

    public CellAssertion<String, ?> withoutValue() {
        return new EmptyCellAssertion(cellAddress);
    }

    public BooleanCellAssertionBuilder withBoolean() {
        return new BooleanCellAssertionBuilder(cellAddress);
    }

    public static class BooleanCellAssertionBuilder {
        private final String cellAddress;

        public BooleanCellAssertionBuilder(String cellAddress) {
            this.cellAddress = cellAddress;
        }

        public CellAssertion<Boolean, ?> equalTo(boolean expectedValue) {
            return new BooleanCellAssertion(cellAddress, expectedValue);
        }

        public CellAssertion<Boolean, ?> isTrue() {
            return new BooleanCellAssertion(cellAddress, true);
        }

        public CellAssertion<Boolean, ?> isFalse() {
            return new BooleanCellAssertion(cellAddress, false);
        }
    }

    public ErrorCellAssertionBuilder withErrorText() {
        return new ErrorCellAssertionBuilder(cellAddress);
    }

    public static class ErrorCellAssertionBuilder {
        private final String cellAddress;

        public ErrorCellAssertionBuilder(String cellAddress) {
            this.cellAddress = cellAddress;
        }

        public ErrorCellAssertion equalTo(String expectedText) {
            return new ErrorTextEqualsCellAssertion(cellAddress, expectedText);
        }

        public ErrorCellAssertion contains(String containsText) {
            return new ErrorTextContainsCellAssertion(cellAddress, containsText);
        }
    }
}
