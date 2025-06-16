package excelAssertions;

import org.junit.jupiter.api.Test;
import org.opentest4j.MultipleFailuresError;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.file.Files;

import static excelAssertions.ExcelAssertionBuilder.*;
import static org.assertj.core.api.Assertions.*;


class ExcelSoftAssertionTest {

    @Test
    @lombok.SneakyThrows
    void testMultipleExcelCellFailures() {
        // Assume "financials_with_errors.xlsx" exists for this test.
        // - A1 contains "Annual Report" (instead of "Quarterly Report")
        // - B5 contains 150.80 (outside the 0.01 tolerance of 150.75)
        // - B12 formula result is correct.
        File excelFile = generateQuarterlyReport();

        // The test will execute all `has()` checks and report all failures at the end.
        assertThatThrownBy(() -> {
            try (var assertThatExcelFile = assertThatExcel(excelFile)) {
                assertThatExcelFile
                        //.has(StringCell("A1").equalsIgnoreCase("Quarterly Report")) // This will fail
                        .has(cellAt("B5").withNumber().isCloseTo(160.75, offset(0.01)))     // This will also fail
                //.has(FormulaCell("B12").withResult(12500.50))           // This will pass
                ;

            }
        })
                .isInstanceOf(MultipleFailuresError.class)
                .hasMessageContaining("Multiple Failures (2 failures)"); // AssertJ wraps multiple errors
    }

    @Test
    @lombok.SneakyThrows
    void testAllAssertionsPass() {
        File excelFile = generateQuarterlyReport();
        try (var assertThatExcelFile = assertThatExcel(excelFile)) {
            assertThatExcelFile
                    .inSheet(0)
                    //.has(StringCell("A1").contains("quarterly"))
                    .has(cellAt("B5").withNumber().isCloseTo(150.0, withinPercentage(1)).withFormat("0.000"))
                    .has(cellAt("B5").withNumber().isCloseTo(150.75, offset(0.01)).withFormat("0.000"))
                    .has(cellAt("B6").withoutValue())
            //.has(FormulaCell("B12").withFormulaText("10+B5").withResult(160.75))
            ;
        }
    }

    private static File generateQuarterlyReport() throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Report");

        // A1 = "Quarterly Report"
        sheet.createRow(0).createCell(0).setCellValue("Quarterly Report");

        // B5 = 150.75 with 3 decimal digits
        var b5 = sheet.createRow(4).createCell(1);
        b5.setCellValue(150.7501);
        DataFormat dataFormat = workbook.createDataFormat();
        CellStyle number3Style = workbook.createCellStyle();
        number3Style.setDataFormat(dataFormat.getFormat("0.000"));
        b5.setCellStyle(number3Style);


        // B12 = "=10+B5"
        sheet.createRow(11).createCell(1).setCellFormula("10+B5");

        // Evaluate the formula so it shows a result in some viewers
        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
        evaluator.evaluateFormulaCell(sheet.getRow(11).getCell(1));

        // Save to temporary file
        var tempFile = Files.createTempFile("QuarterlyReport-", ".xlsx").toFile();
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            workbook.write(out);
        }

        workbook.close();

        //if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(tempFile);
        //else throw new UnsupportedOperationException("Desktop opening not supported.");

        return tempFile;
    }
}