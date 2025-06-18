package excelAssertions;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.assertj.core.api.SoftAssertions;

import java.io.*;

public class ExcelAssert implements AutoCloseable {
    private final Workbook workbook;
    private final SoftAssertions softly;
    private Sheet sheet;

    ExcelAssert(Workbook workbook) {
        this.softly = new SoftAssertions();
        this.workbook = workbook;
        this.sheet = workbook.getSheetAt(0);
    }

    public ExcelAssert inSheet(int index) {
        sheet = workbook.getSheetAt(index);
        return this;
    }

    public ExcelAssert inSheet(String sheetName) {
        sheet = workbook.getSheet(sheetName);
        return this;
    }

    public ExcelAssert has(CellAssertion<?, ?> cellAssertion) {
        cellAssertion.doAssert(cellAssertion.getCell(sheet), softly);
        return this;
    }

    public ExcelAssert have(CellAssertion<?, ?>... cellAssertions) {
        for (var ca : cellAssertions)
            ca.doAssert(ca.getCell(sheet), softly);
        return this;
    }

    @Override
    public void close() {
        try {
            workbook.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e); // wrap to avoid checked exceptions
        }

        softly.assertAll();
    }
}