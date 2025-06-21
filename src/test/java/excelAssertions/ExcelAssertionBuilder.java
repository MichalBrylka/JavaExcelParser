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

            public ValueCellAssertion<Double, ?> closeTo(Double expectedValue, Percentage percentage) {
                return new CloseToPercentNumberCellAssertion(cellAddress, expectedValue, percentage);
            }

            public ValueCellAssertion<Double, ?> closeTo(Double expectedValue, Offset<Double> offset) {
                return new CloseToOffsetNumberCellAssertion(cellAddress, expectedValue, offset);
            }

            public ValueCellAssertion<Double, ?> equalTo(Double expectedValue) {
                return new EqualToNumberCellAssertion(cellAddress, expectedValue);
            }

            public ValueCellAssertion<Double, ?> greaterThan(Double expectedValue) {
                return new GreaterThanNumberCellAssertion(cellAddress, expectedValue);
            }

            public ValueCellAssertion<Double, ?> greaterThanOrEqualTo(Double expectedValue) {
                return new GreaterThanOrEqualToNumberCellAssertion(cellAddress, expectedValue);
            }

            public ValueCellAssertion<Double, ?> lessThan(Double expectedValue) {
                return new LessThanNumberCellAssertion(cellAddress, expectedValue);
            }

            public ValueCellAssertion<Double, ?> lessThanOrEqualTo(Double expectedValue) {
                return new LessThanOrEqualToNumberCellAssertion(cellAddress, expectedValue);
            }
        }

        public BooleanCellAssertionBuilder withBoolean() {
            return new BooleanCellAssertionBuilder(cellAddress);
        }

        public static class BooleanCellAssertionBuilder {
            private final String cellAddress;

            public BooleanCellAssertionBuilder(String cellAddress) {
                this.cellAddress = cellAddress;
            }

            public ValueCellAssertion<Boolean, ?> equalTo(boolean expectedValue) {
                return new BooleanCellAssertion(cellAddress, expectedValue);
            }

            public ValueCellAssertion<Boolean, ?> ofTrue() {
                return new BooleanCellAssertion(cellAddress, true);
            }

            public ValueCellAssertion<Boolean, ?> ofFalse() {
                return new BooleanCellAssertion(cellAddress, false);
            }
        }

        public TextCellAssertion withText(TextAssertion<?> textAssertion) {
            return new TextCellAssertion(cellAddress, textAssertion);
        }

        public TextCellAssertion withText(String expectedText) {
            return new TextCellAssertion(cellAddress, new EqualsTextAssertion(expectedText, false, false));
        }

        public FormulaTextCellAssertion withFormulaText(TextAssertion<?> textAssertion) {
            return new FormulaTextCellAssertion(cellAddress, textAssertion);
        }

        public ErrorTextCellAssertion withErrorText(TextAssertion<?> textAssertion) {
            return new ErrorTextCellAssertion(cellAddress, textAssertion);
        }

        public EmptyCellAssertion empty() {
            return new EmptyCellAssertion(cellAddress);
        }

        public SimpleCellAssertion withoutValueCheck() {
            return new SimpleCellAssertion(cellAddress);
        }
    }
}
