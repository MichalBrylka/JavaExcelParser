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

    public static ExcelCellAssertionBuilder cellAt(String cellAddress) {
        return new ExcelCellAssertionBuilder(cellAddress);
    }

    public static EqualsTextAssertion equalTo(String expectedText) {
        return new EqualsTextAssertion(expectedText, false, false);
    }

    public static ContainsTextAssertion containing(String containsText) {
        return new ContainsTextAssertion(containsText, false);
    }

    public static PatternTextAssertion matching(String pattern) {
        return new PatternTextAssertion(pattern, false, false);
    }


    public static class ExcelCellAssertionBuilder {
        private final String cellAddress;

        private ExcelCellAssertionBuilder(String cellAddress) {
            this.cellAddress = cellAddress;
        }

        public NumberCellAssertionBuilder withNumber() {
            return new NumberCellAssertionBuilder(cellAddress);
        }

        public static class NumberCellAssertionBuilder {
            private final String cellAddress;

            public NumberCellAssertionBuilder(String cellAddress) {
                this.cellAddress = cellAddress;
            }

            public CellAssertion<Double, ?> closeTo(Double expectedValue, Percentage percentage) {
                return new CloseToPercentNumberCellAssertion(cellAddress, expectedValue, percentage);
            }

            public CellAssertion<Double, ?> closeTo(Double expectedValue, Offset<Double> offset) {
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

        public EmptyCellAssertion withoutValue() {
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

        public ErrorTextCellAssertion withErrorText(TextAssertion<?> textAssertion) {
            return new ErrorTextCellAssertion(cellAddress, textAssertion);
        }
    }
}
