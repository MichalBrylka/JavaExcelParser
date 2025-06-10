package excelAssertions;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.assertj.core.api.SoftAssertions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class ExcelAssert {
    private final Workbook workbook;
    private final SoftAssertions softly;
    private Sheet sheet;


    private ExcelAssert(File actual) {
        this.softly = new SoftAssertions();
        try (FileInputStream fis = new FileInputStream(actual)) {
            this.workbook = WorkbookFactory.create(fis);
            this.sheet = workbook.getSheetAt(0);
        } catch (IOException e) {
            // A failure to open the file should still fail fast.
            throw new AssertionError("Failed to read Excel file: <" + actual.getName() + ">. Reason: " + e.getMessage(), e);
        }
    }

    /**
     * Entry point for soft assertions on an Excel file.
     * Best used with a try-with-resources statement to ensure all assertions are evaluated.
     *
     * @param file The Excel file to be asserted.
     * @return An {@link ExcelAssert} instance that is AutoCloseable.
     */
    public static ExcelAssert assertThatExcel(File file) {
        return new ExcelAssert(file);
    }

    public static ExcelAssert assertThatExcel(String file) {
        return new ExcelAssert(new File(file));
    }

    public ExcelAssert inSheet(int index) {
        sheet = workbook.getSheetAt(index);
        return this;
    }

    public ExcelAssert inSheet(String sheetName) {
        sheet = workbook.getSheet(sheetName);
        return this;
    }

    /**
     * Adds a cell-specific assertion to the list of checks to be performed.
     * Failures are collected and reported at the end.
     *
     * @param cellAssertion The specific cell assertion to perform (e.g., NumberCell, StringCell).
     * @return this {@link ExcelAssert} object to allow for chaining multiple assertions.
     */
    public ExcelAssert has(CellAssertion<?> cellAssertion) {
        cellAssertion.doAssert(cellAssertion.getCell(sheet), softly);
        return this;
    }

    public void check() {
        softly.assertAll();
    }
}