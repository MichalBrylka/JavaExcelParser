package excelAssertions;

import org.junit.jupiter.api.*;
import org.opentest4j.MultipleFailuresError;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

import static excelAssertions.ExcelAssertionBuilder.*;
import static org.assertj.core.api.Assertions.*;

class ExcelSoftAssertionTest {
    private ExcelAssert assertThatExcelFile;
    private File exampleFile;


    @Test
    @lombok.SneakyThrows
    void testMultipleExcelCellFailures() {
        usingNewExcelFile();

        // The test will execute all `has()` checks and report all failures at the end.
        assertThatThrownBy(() -> {
            assertThatExcelFile
                    //.has(StringCell("A1").equalsIgnoreCase("Quarterly Report")) // This will fail
                    .has(cellAt("B5").withNumber().closeTo(160.75, offset(0.01)))     // This will also fail
            //.has(FormulaCell("B12").withResult(12500.50))           // This will pass
            ;
        })
                .isInstanceOf(MultipleFailuresError.class)
                .hasMessageContaining("Multiple Failures (2 failures)"); // AssertJ wraps multiple errors
    }

    @Test
    void testAllAssertionsPass() {
        usingNewExcelFile();
        assertThatExcelFile
                .inSheet("Numbers").have(
//                cellAt("A1").withNumber()..withFormat("0.00"),
//                cellAt("A2").withNumber().     .withFormat("0.0000%"),
//                cellAt("A3").withNumber().     .withFormat("0.00"),
//                cellAt("A4").withNumber().     .withFormat("0.00000000"),
//                cellAt("A5").withNumber().     .withFormat("#,##0"),
//                cellAt("A6").withNumber().     .withFormat("0.0000"),
//                cellAt("A7").withNumber().     .withFormat("0.0000"),
//                cellAt("A8").withNumber().     .withFormat("0.00"),

                        //cellAt("B5").withNumber().closeTo(150.0, withinPercentage(1)).withFormat("0.000"),
                        //cellAt("B5").withNumber().closeTo(150.75, offset(0.01)).withFormat("0.000"),
                        cellAt("B1").withoutValue()
                )
                .inSheet("Errors").have(
                        cellAt("A1").withErrorText(containing("div/0").ignoreCase()),
                        cellAt("A2").withErrorText(equalTo("#N/A").caseSensitive()),
                        cellAt("A3").withErrorText(equalTo("#NUM!\n").ignoreNewLines()),
                        cellAt("A4").withErrorText(matching("^#VaL\\w[a-z]!$").ignoreCase())
                )
        ;

    }

    @lombok.SneakyThrows
    void usingNewExcelFile() {
        exampleFile = Files.createTempFile("Example-", ".xlsx").toFile();
        try (FileOutputStream out = new FileOutputStream(exampleFile)) {
            generateTestExcelFile(out);
            //java.awt.Desktop.getDesktop().open(tempFile);
        }
        assertThatExcelFile = assertThatExcel(exampleFile);
    }

    @AfterEach
    @lombok.SneakyThrows
    void tearDown() {
        if (assertThatExcelFile != null) {
            assertThatExcelFile.close();
            assertThatExcelFile = null;
        }

        if (exampleFile != null)
            Files.deleteIfExists(exampleFile.toPath());
    }

    private static void generateTestExcelFile(OutputStream output) throws IOException {
        try (var workbook = new XSSFWorkbook()) {

            Map<String, java.util.List<CellBase>> sheets = new LinkedHashMap<>();

            sheets.put("Numbers", java.util.List.of(
                    new FormulaCell("1+1", "0.00"),
                    new FormulaCell("100/3", "0.0000%"),
                    new NumberCell(Float.MAX_VALUE, "0.00"),
                    new NumberCell(Float.MIN_VALUE, "0.00000000"),
                    new FormulaCell("-9999999", "#,##0"),
                    new FormulaCell("SQRT(2)", "0.0000"),
                    new FormulaCell("PI()", "0.0000"),
                    new FormulaCell("RAND()*100", "0.00")
            ));

            sheets.put("Strings", java.util.List.of(
                    new TextCell("Quarterly Report"),
                    new FormulaCell("""
                            "Hello "&"World\""""),
                    new FormulaCell("""
                            FIXED(123456.789, 2, FALSE)"""),
                    new TextCell("\"\""),
                    new FormulaCell("""
                            "Line1"&CHAR(10)&"Line2\""""),
                    new FormulaCell("""
                            "123" & "456\""""),
                    new FormulaCell("""
                            "=" & "SUM(1,2)"\s""")
            ));

            sheets.put("Dates", java.util.List.of(
                    new FormulaCell("DATE(2023,1,1)", "yyyy-mm-dd"),
                    new FormulaCell("DATE(1900,1,1)", "yyyy-mm-dd"),
                    new FormulaCell("TODAY()", "yyyy-mm-dd"),
                    new FormulaCell("DATE(2024,2,29)", "yyyy-mm-dd"),
                    new FormulaCell("DATE(1999,12,31)", "yyyy-mm-dd"),
                    new FormulaCell("EDATE(TODAY(),-1)", "yyyy-mm-dd")
            ));

            sheets.put("Times", java.util.List.of(
                    new FormulaCell("TIME(12,0,0)", "hh:mm"),
                    new FormulaCell("TIME(23,59,59)", "hh:mm:ss"),
                    new FormulaCell("NOW()-TODAY()", "hh:mm:ss"),
                    new FormulaCell("TIME(0,0,0)", "hh:mm:ss"),
                    new FormulaCell("TIME(7,30,15)", "hh:mm:ss AM/PM"),
                    new FormulaCell("MOD(NOW(),1)", "hh:mm:ss")
            ));

            sheets.put("DateTimes", java.util.List.of(
                    new FormulaCell("NOW()", "yyyy-mm-dd hh:mm:ss"),
                    new FormulaCell("DATE(2025,6,17)+TIME(15,30,0)", "[$-en-US]yyyy-mmm-dd hh:mm:ss;@"),
                    new FormulaCell("NOW()+1/24", "yyyy-mm-dd hh:mm:ss"),
                    new FormulaCell("NOW()-1/24", "yyyy-mmm-dd hh:mm:ss"),
                    new FormulaCell("TODAY()+TIME(23,59,59)", "yyyy-mm-dd hh:mm:ss")
            ));

            sheets.put("Booleans", java.util.List.of(
                    new FormulaCell("1=1"),
                    new FormulaCell("ISNUMBER(123)"),
                    new FormulaCell("FALSE"),
                    new FormulaCell("1>2"),
                    new FormulaCell("NOT(TRUE)"),
                    new FormulaCell("AND(TRUE,FALSE)"),
                    new FormulaCell("OR(TRUE,FALSE)")
            ));

            sheets.put("Errors", List.of(
                    new FormulaCell("1/0"),
                    new FormulaCell("NA()"),
                    new FormulaCell("SQRT(-1)"),
                    new FormulaCell("""
                            1+"a\""""),
                    new FormulaCell("""
                            INDIRECT("A" & (2^20+1))"""),
                    new FormulaCell("XYZ()")
            ));

            for (var entry : sheets.entrySet()) {
                Sheet sheet = workbook.createSheet(entry.getKey());
                int rowIdx = 0;

                for (CellBase cellData : entry.getValue()) {
                    Row row = sheet.createRow(rowIdx++);
                    Cell cell = row.createCell(0);

                    switch (cellData) {
                        case TextCell c -> cell.setCellValue(c.text);
                        case NumberCell n -> cell.setCellValue(n.number);
                        case FormulaCell f -> cell.setCellFormula(f.formula);
                        default -> throw new IllegalStateException("Unexpected cell type: " + cell);
                    }

                    if (cellData.format() instanceof String format) {
                        CellStyle style = workbook.createCellStyle();
                        style.setDataFormat(workbook.createDataFormat().getFormat(format));
                        cell.setCellStyle(style);
                    }
                }
            }

            workbook.write(output);
        }
    }

    interface CellBase {
        String format();
    }

    record TextCell(String text, String format) implements CellBase {
        TextCell(String text) {
            this(text, null);
        }
    }

    record NumberCell(double number, String format) implements CellBase {
        NumberCell(double number) {
            this(number, null);
        }
    }

    record FormulaCell(String formula, String format) implements CellBase {
        FormulaCell(String formula) {
            this(formula, null);
        }
    }
}