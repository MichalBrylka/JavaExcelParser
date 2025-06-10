package excelAssertions;

public class CellAssertions {

    public static NumberCellAssertion NumberCell(String cellAddress) {
        return new NumberCellAssertion(cellAddress);
    }

    public static StringCellAssertion StringCell(String cellAddress) {
        return new StringCellAssertion(cellAddress);
    }

    /*public static LocalDateTimeCellAssertion LocalDateTimeCell(String cellAddress) {
        return new LocalDateTimeCellAssertion(cellAddress);
    }



    public static FormulaCellAssertion FormulaCell(String cellAddress) {
        return new FormulaCellAssertion(cellAddress);
    }*/
}