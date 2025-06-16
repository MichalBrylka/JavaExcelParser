package excelAssertions;

import org.apache.poi.ss.usermodel.*;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.data.Offset;
import org.jetbrains.annotations.NotNull;
import org.apache.poi.ss.formula.eval.ErrorEval;

/*public final class FormulaCellAssertion extends CellAssertion<FormulaCellAssertion> {
    private String expectedFormulaText;
    private Object expectedResult;

    public FormulaCellAssertion(String cellAddress) {
        super(cellAddress);
    }

    public FormulaCellAssertion withFormulaText(String expectedFormulaText) {
        this.expectedFormulaText = expectedFormulaText;
        return self();
    }

    public FormulaCellAssertion withResult(Object expectedResult) {
        this.expectedResult = expectedResult;
        return self();
    }

    @Override
    protected boolean doAssertCore(Cell cell, SoftAssertions softly) {
        if (cell == null || cell.getCellType() != CellType.FORMULA) {
            softly.assertThat(cell)
                    .as("Cell " + cellAddress + " is not a formula cell.")
                    .isNotNull()
                    .extracting(Cell::getCellType)
                    .isEqualTo(CellType.FORMULA);
            return true;
        }

        boolean assertionsAdded = false;

        if (expectedFormulaText != null) {
            String actualFormulaText = cell.getCellFormula();
            softly.assertThat(actualFormulaText).isEqualToIgnoringCase(expectedFormulaText);
            assertionsAdded = true;

        }
        if (expectedResult != null) {
            FormulaEvaluator evaluator = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();
            CellValue cellValue = evaluator.evaluate(cell);
            assertResult(cellValue, expectedResult, softly);
            assertionsAdded = true;
        }
        return assertionsAdded;
    }


    private void assertResult(CellValue actualCell, Object expected, SoftAssertions softly) {
        switch (actualCell.getCellType()) {
            case NUMERIC:
                if (typeMismatchFound(Number.class, expected, softly))
                    return;

                double actualNumber = actualCell.getNumberValue();
                double expectedNumber = ((Number) expected).doubleValue();
                softly.assertThat(actualNumber).isCloseTo(expectedNumber, Offset.offset(0.001));

                break;
            case STRING:
                if (typeMismatchFound(String.class, expected, softly))
                    return;

                String actualString = actualCell.getStringValue();
                softly.assertThat(actualString).isEqualTo(expected);

                break;
            case BOOLEAN:
                if (typeMismatchFound(Boolean.class, expected, softly))
                    return;

                boolean actualBoolean = actualCell.getBooleanValue();
                softly.assertThat(actualBoolean).isEqualTo(expected);

                break;
            case ERROR:
                if (typeMismatchFound(String.class, expected, softly))
                    return;

                String expectedText = (String) expected;
                byte errorCode = actualCell.getErrorValue();
                String errorText = ErrorEval.getText(errorCode);

                softly.assertThat(errorText).isEqualToIgnoringWhitespace(expectedText);
            default:
                softly.fail(String.format("Unhandled formula result type '%s' for cell '%s'", actualCell.getCellType(), cellAddress));
        }
    }

    private <T> boolean typeMismatchFound(@NotNull Class<T> expectedClass, @NotNull Object expected, SoftAssertions softly) {
        if (!expectedClass.isInstance(expected)) {
            softly.assertThat(expected)
                    .as(String.format(
                                    "Cell '%s' formula result expected type is <%s>, but expected result type was <%s>",
                                    cellAddress, expectedClass.getSimpleName(), expected.getClass().getSimpleName()
                            )
                    )
                    .isInstanceOf(expectedClass);
            return true;
        } else return false;
    }
}*/
