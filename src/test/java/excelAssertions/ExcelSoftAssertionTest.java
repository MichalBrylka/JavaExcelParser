package excelAssertions;

import excelAssertions.io.*;
import org.assertj.core.data.Offset;
import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.*;
import org.opentest4j.MultipleFailuresError;
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
        assertThatThrownBy(() -> assertThatExcelFile
                .has(
                        cellAt("B5").withNumber(closeTo(160.75, offset(0.01)))
                ))
                .isInstanceOf(MultipleFailuresError.class)
                .hasMessageContaining("Multiple Failures (2 failures)"); // AssertJ wraps multiple errors
    }

    @Test
    void testAllAssertionsPass() {
        usingNewExcelFile();
        assertThatExcelFile
                //check empty
                .inSheet(1).has(
                        cellAt("B1").empty()
                )

                //numbers
                .inSheet("Numbers").have(
                        cellAt("A1").withNumber(equalTo(2.0)).withFormat("0.00"),
                        cellAt("A2").withNumber(greaterThan(33)).withFormat("0.0000%"),
                        cellAt("A3").withNumber(greaterThanOrEqualTo(10)).withFormat("0.00"),
                        cellAt("A4").withNumber(lessThan(0.0000001)).withFormat("0.00000000"),
                        cellAt("A5").withNumber(lessThanOrEqualTo(-9999999)).withFormat("#,##0"),
                        cellAt("A6").withNumber(closeTo(1.4142, Offset.offset(0.0001))).withFormat("0.0000"),
                        cellAt("A7").withNumber(closeTo(3.14, Percentage.withPercentage(0.1))).withFormat("0.0000"),

                        cellAt("A1").withNumber(withinRange(1.99, 2.01)),
                        cellAt("A1").withNumber(withinRange(1.99999, 2.00001, true, true)),
                        cellAt("A1").withNumber(outsideRange(2.01, 2.02)),
                        cellAt("A1").withNumber(outsideRange(2.00, 2.02, true, true))
                )

                //formats
                .inSheet("Numbers").have(
                        cellAt("A1").exists().withFormat("0.00"),
                        cellAt("A2").exists().withFormat(equalTo("0.0000%")),
                        cellAt("A4").exists().withFormat(containing("00000000")),
                        cellAt("A5").exists().withFormat(matching(".*##\\d"))
                )

                //formulas
                .inSheet("Strings").have(
                        cellAt("A2").withFormulaText(containing("&\"World\"").caseSensitive()),
                        cellAt("A3").withFormulaText(equalTo("""
                                TEXT(123456,"##0° 00' 00''")""").ignoreCase()),
                        cellAt("A5").withFormulaText(containing("char").ignoreCase()),
                        cellAt("A6").withFormulaText(matching(".*[1-3]{3}.*").dotallMode())
                )

                //texts
                .inSheet("Strings").have(
                        cellAt("A1").withText(containing("report").ignoreCase()),
                        cellAt("A2").withText(equalTo("Hello World").caseSensitive()),
                        cellAt("A3").withText("123456"),
                        cellAt("A4").withText(equalTo("\"\"")),
                        cellAt("A5").withText(equalTo("Line1\n\nline2").ignoreCase().ignoreNewLines()),
                        cellAt("A5").withText(matching("Line1.*line2").ignoreCase().dotallMode()),
                        cellAt("A6").withText(matching("^[1-6]{6}$")),
                        cellAt("A7").withText(matching("""
                                ^=sUm\\(\\d+,\\d+\\)$""").ignoreCase())
                )

                //errors
                .inSheet("Errors").have(
                        cellAt("A1").withErrorText(containing("div/0").ignoreCase()),
                        cellAt("A2").withErrorText(equalTo("#N/A").caseSensitive()),
                        cellAt("A3").withErrorText(equalTo("#NUM!\n").ignoreNewLines()),
                        cellAt("A4").withErrorText(matching("^#VaL\\w[a-z]!$").ignoreCase())
                )

                //booleans
                .inSheet("Booleans").have(
                        cellAt("A1").withBoolean(ofTrue()),
                        cellAt("A2").withBoolean(equalTo(true)),
                        cellAt("A3").withBoolean(ofFalse())
                )

                //comments
                .inSheet("Comments").have(
                        cellAt("A1").exists().withComment("FORMAT"),
                        cellAt("A1").exists().withComment(equalTo("format").ignoreCase()),
                        cellAt("A5").exists().withComment(containing("MpT").ignoreCase()),
                        cellAt("A9").exists().withComment(matching("\\waL[ue]{2}").ignoreCase())
                )
        ;
    }

    @lombok.SneakyThrows
    void usingNewExcelFile() {
        exampleFile = Files.createTempFile("Example-", ".xlsx").toFile();
        try (FileOutputStream out = new FileOutputStream(exampleFile)) {
            generateTestExcelFile(out);
            //java.awt.Desktop.getDesktop().open(exampleFile);
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

        if (exampleFile != null) Files.deleteIfExists(exampleFile.toPath());
    }

    private static void generateTestExcelFile(OutputStream output) throws IOException {
        try (var workbook = new XSSFWorkbook()) {

            List<SheetEntry> sheetEntries = new ArrayList<>();

            sheetEntries.add(new SheetEntry("Numbers", List.of(
                    FormulaCellEntry.ofNoValue("A1", "1+1", "0.00"),
                    FormulaCellEntry.ofNoValue("A2", "100/3", "0.0000%"),
                    new NumberCellEntry("A3", (double) Float.MAX_VALUE, "0.00"),
                    new NumberCellEntry("A4", (double) Float.MIN_VALUE, "0.00000000"),
                    FormulaCellEntry.ofNoValue("A5", "-9999999", "#,##0"),
                    FormulaCellEntry.ofNoValue("A6", "SQRT(2)", "0.0000"),
                    FormulaCellEntry.ofNoValue("A7", "PI()", "0.0000"),
                    FormulaCellEntry.ofNoValue("A8", "RAND()*100", "0.00")
            )));


            sheetEntries.add(new SheetEntry("Strings", List.of(
                    new TextCellEntry("A1", "Quarterly Report"),
                    FormulaCellEntry.ofNoValue("A2", """
                            "Hello "&"World\""""),
                    FormulaCellEntry.ofNoValue("A3", """
                            TEXT(123456,"##0° 00' 00''")"""),
                    new TextCellEntry("A4", "\"\""),
                    FormulaCellEntry.ofNoValue("A5", """
                            "Line1"&CHAR(10)&"Line2\""""),
                    FormulaCellEntry.ofNoValue("A6", """
                            "123" & "456\""""),
                    FormulaCellEntry.ofNoValue("A7", """
                            "=" & "SUM(1,2)"\s""")
            )));


            sheetEntries.add(new SheetEntry("Dates", List.of(
                    FormulaCellEntry.ofNoValue("A1", "DATE(2023,1,1)", "yyyy-mm-dd"),
                    FormulaCellEntry.ofNoValue("A2", "DATE(1900,1,1)", "yyyy-mm-dd"),
                    FormulaCellEntry.ofNoValue("A3", "TODAY()", "yyyy-mm-dd"),
                    FormulaCellEntry.ofNoValue("A4", "DATE(2024,2,29)", "yyyy-mm-dd"),
                    FormulaCellEntry.ofNoValue("A5", "DATE(1999,12,31)", "yyyy-mm-dd"),
                    FormulaCellEntry.ofNoValue("A6", "EDATE(TODAY(),-1)", "yyyy-mm-dd")
            )));

            sheetEntries.add(new SheetEntry("Times", List.of(
                    FormulaCellEntry.ofNoValue("A1", "TIME(12,0,0)", "hh:mm"),
                    FormulaCellEntry.ofNoValue("A2", "TIME(23,59,59)", "hh:mm:ss"),
                    FormulaCellEntry.ofNoValue("A3", "NOW()-TODAY()", "hh:mm:ss"),
                    FormulaCellEntry.ofNoValue("A4", "TIME(0,0,0)", "hh:mm:ss"),
                    FormulaCellEntry.ofNoValue("A5", "TIME(7,30,15)", "hh:mm:ss AM/PM"),
                    FormulaCellEntry.ofNoValue("A6", "MOD(NOW(),1)", "hh:mm:ss")
            )));

            sheetEntries.add(new SheetEntry("DateTimes", List.of(
                    FormulaCellEntry.ofNoValue("A1", "NOW()", "yyyy-mm-dd hh:mm:ss"),
                    FormulaCellEntry.ofNoValue("A2", "DATE(2025,6,17)+TIME(15,30,0)", "[$-en-US]yyyy-mmm-dd hh:mm:ss;@"),
                    FormulaCellEntry.ofNoValue("A3", "NOW()+1/24", "yyyy-mm-dd hh:mm:ss"),
                    FormulaCellEntry.ofNoValue("A4", "NOW()-1/24", "yyyy-mmm-dd hh:mm:ss"),
                    FormulaCellEntry.ofNoValue("A5", "TODAY()+TIME(23,59,59)", "yyyy-mm-dd hh:mm:ss")
            )));


            sheetEntries.add(new SheetEntry("Booleans", List.of(
                    FormulaCellEntry.ofNoValue("A1", "1=1"),
                    FormulaCellEntry.ofNoValue("A2", "ISNUMBER(123)"),
                    FormulaCellEntry.ofNoValue("A3", "FALSE"),
                    FormulaCellEntry.ofNoValue("A4", "1>2"),
                    FormulaCellEntry.ofNoValue("A5", "NOT(TRUE)"),
                    FormulaCellEntry.ofNoValue("A6", "AND(TRUE,FALSE)"),
                    FormulaCellEntry.ofNoValue("A7", "OR(TRUE,FALSE)")
            )));


            sheetEntries.add(new SheetEntry("Errors", List.of(
                    FormulaCellEntry.ofNoValue("A1", "1/0"),
                    FormulaCellEntry.ofNoValue("A2", "NA()"),
                    FormulaCellEntry.ofNoValue("A3", "SQRT(-1)"),
                    FormulaCellEntry.ofNoValue("A4", """
                            1+"a\""""),
                    FormulaCellEntry.ofNoValue("A5", """
                            INDIRECT("A" & (2^20+1))"""),
                    FormulaCellEntry.ofNoValue("A6", "XYZ()")
            )));


            sheetEntries.add(new SheetEntry("Headers", List.of(
                    new TextCellEntry("E3", "Header"),
                    new TextCellEntry("E4", "First"),
                    new TextCellEntry("E5", "Second"),
                    new TextCellEntry("E6", "Third")
            )));

            sheetEntries.add(new SheetEntry("Comments", List.of(
                    new TextCellEntry("A1", "COMMENT1", null, "FORMAT"),
                    new TextCellEntry("A5", "COMMENT2", null, "EMPTY"),
                    new TextCellEntry("A9", "COMMENT3", null, "VALUE")
            )));


            ExcelOperations.fillWorkbook(workbook, sheetEntries);
            workbook.write(output);
        }
    }
}