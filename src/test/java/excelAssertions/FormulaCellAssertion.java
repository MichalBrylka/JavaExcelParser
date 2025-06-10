package excelAssertions;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;

/*
public final class FormulaCellAssertion extends CellAssertion<FormulaCellAssertion> {

    private String expectedFormulaText;
    private Object expectedResult;

    public FormulaCellAssertion(String cellAddress) {
        super(cellAddress);
    }

    public FormulaCellAssertion withText(String expectedFormulaText) {
        this.expectedFormulaText = expectedFormulaText;
        return self();
    }

    public FormulaCellAssertion withResult(Object expectedResult) {
        this.expectedResult = expectedResult;
        return self();
    }

    @Override
    public void doAssert(Workbook workbook) {
        Cell cell = getCell(workbook, cellAddress);

        if (cell == null || cell.getCellType() != CellType.FORMULA) {
            throw new AssertionError(String.format("Cell '%s' is not a formula cell.", cellAddress));
        }

        // 1. Assert on the formula text
        if (expectedFormulaText != null) {
            String actualFormulaText = cell.getCellFormula();
            if (!expectedFormulaText.equals(actualFormulaText)) {
                throw new AssertionError(String.format(
                        "Cell '%s' formula text <%s> is not equal to expected <%s>",
                        cellAddress, actualFormulaText, expectedFormulaText
                ));
            }
        }

        // 2. Assert on the formula result
        if (expectedResult != null) {
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            CellValue cellValue = evaluator.evaluate(cell);
            assertResult(cellValue, expectedResult);
        }
    }

    private void assertResult(CellValue actual, Object expected) {
        switch (actual.getCellType()) {
            case NUMERIC:
                if (!(expected instanceof Number)) {
                    failTypeMismatch("Number", expected);
                }
                double actualNumber = actual.getNumberValue();
                double expectedNumber = ((Number) expected).doubleValue();
                if (Double.compare(actualNumber, expectedNumber) != 0) {
                    failResultMismatch(actualNumber, expected);
                }
                break;
            case STRING:
                if (!(expected instanceof String)) {
                    failTypeMismatch("String", expected);
                }
                String actualString = actual.getStringValue();
                if (!actualString.equals(expected)) {
                    failResultMismatch(actualString, expected);
                }
                break;
            case BOOLEAN:
                if (!(expected instanceof Boolean)) {
                    failTypeMismatch("Boolean", expected);
                }
                boolean actualBoolean = actual.getBooleanValue();
                if (actualBoolean != (Boolean) expected) {
                    failResultMismatch(actualBoolean, expected);
                }
                break;
            case ERROR:
                throw new AssertionError(String.format("Cell '%s' formula resulted in an error: %s",
                        cellAddress, FormulaError.forInt(actual.getErrorValue()).getString()));
            default:
                throw new AssertionError(String.format("Unhandled formula result type '%s' for cell '%s'",
                        actual.getCellType(), cellAddress));
        }
    }

    private void failTypeMismatch(String actualType, Object expected) {
        throw new AssertionError(String.format(
                "Cell '%s' formula result type is <%s>, but expected result type was <%s>",
                cellAddress, actualType, expected.getClass().getSimpleName()
        ));
    }

    private void failResultMismatch(Object actual, Object expected) {
        throw new AssertionError(String.format(
                "Cell '%s' formula result <%s> did not match expected result <%s>",
                cellAddress, actual, expected
        ));
    }
}*/
