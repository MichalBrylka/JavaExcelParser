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

    public static NumberAssertion equalTo(double expected) {return new EqualToNumberAssertion(expected);}

    public static NumberAssertion greaterThan(double threshold) {return new GreaterThanNumberAssertion(threshold);}

    public static NumberAssertion greaterThanOrEqualTo(double threshold) {return new GreaterThanOrEqualToNumberAssertion(threshold);}

    public static NumberAssertion lessThan(double threshold) {return new LessThanNumberAssertion(threshold);}

    public static NumberAssertion lessThanOrEqualTo(double threshold) {return new LessThanOrEqualToNumberAssertion(threshold);}

    public static NumberAssertion closeTo(double expected, Offset<Double> offset) {return new CloseToOffsetNumberAssertion(expected, offset);}

    public static NumberAssertion closeTo(double expected, Percentage percentage) {return new CloseToPercentNumberAssertion(expected, percentage);}

    public static NumberAssertion withinRange(double from, double to) {return new WithinRangeNumberAssertion(from, to);}

    public static NumberAssertion withinRange(double from, double to, boolean exclusiveFrom, boolean exclusiveTo) {return new WithinRangeNumberAssertion(from, to, exclusiveFrom, exclusiveTo);}

    public static NumberAssertion outsideRange(double from, double to) {return new OutsideRangeNumberAssertion(from, to);}

    public static NumberAssertion outsideRange(double from, double to, boolean exclusiveFrom, boolean exclusiveTo) {return new OutsideRangeNumberAssertion(from, to, exclusiveFrom, exclusiveTo);}

    public record BooleanAssertion(boolean expected) {}

    public static BooleanAssertion equalTo(boolean expectedValue) {return new BooleanAssertion(expectedValue);}

    public static BooleanAssertion ofTrue() {return new BooleanAssertion(true);}

    public static BooleanAssertion ofFalse() {return new BooleanAssertion(false);}

    public record ExcelCellAssertionBuilder(String cellAddress) {
        public ValueCellAssertion<Boolean, ?> withBoolean(BooleanAssertion booleanAssertion) {return new BooleanCellAssertion(cellAddress, booleanAssertion.expected);}

        public ValueCellAssertion<Double, ?> withNumber(NumberAssertion numberAssertion) {return new NumberCellAssertion(cellAddress, numberAssertion);}

        public ValueCellAssertion<String, ?> withText(TextAssertion<?> textAssertion) {return new TextCellAssertion(cellAddress, textAssertion);}

        public ValueCellAssertion<String, ?> withText(String expectedText) {return new TextCellAssertion(cellAddress, new EqualsTextAssertion(expectedText, false, false));}

        public ValueCellAssertion<String, ?> withFormulaText(TextAssertion<?> textAssertion) {return new FormulaTextCellAssertion(cellAddress, textAssertion);}

        public ValueCellAssertion<String, ?> withErrorText(TextAssertion<?> textAssertion) {return new ErrorTextCellAssertion(cellAddress, textAssertion);}

        public CellAssertion<?> empty() {return new EmptyCellAssertion(cellAddress);}

        public CellAssertion<?> exists() {return new SimpleCellAssertion(cellAddress);}
    }
}
