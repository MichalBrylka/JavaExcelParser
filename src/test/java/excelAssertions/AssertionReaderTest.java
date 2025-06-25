package excelAssertions;

import excelAssertions.io.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Stream;

import static excelAssertions.ExcelAssertionBuilder.*;

class AssertionReaderTest {
    private ExcelAssert assertThatExcelFile;
    private File assertionsFile;
    private File dummyFile;

    @Test
    void readFrom_ShouldReadAssertionsFromExcel() {
        usingNewExcelFile();
        AssertionReader.readFrom(assertionsFile, assertThatExcelFile);
        var assertions = assertThatExcelFile.getAssertions();

        /*var expectedAssertions = Stream.<ExpAss>of(
              new ExpAss(),
        );*/

        //ExcelAssert.CellAssertionAtSheet
    }

    record ExpAss(@NotNull Object sheetRef, @NotNull CellAssertion<?> assertion) {}

    @lombok.SneakyThrows
    void usingNewExcelFile() {
        assertionsFile = Files.createTempFile("Assertions-", ".xlsx").toFile();
        try (FileOutputStream outAssertions = new FileOutputStream(assertionsFile)) {
            generateAssertionsExcelFile(outAssertions);
            //java.awt.Desktop.getDesktop().open(assertionsFile);
        }

        dummyFile = Files.createTempFile("Dummy-", ".xlsx").toFile();
        try (FileOutputStream outDummy = new FileOutputStream(dummyFile)) {
            generateDummyExcelFile(outDummy);
            //java.awt.Desktop.getDesktop().open(dummyFile);
        }
        assertThatExcelFile = assertThatExcel(dummyFile);
    }

    @AfterEach
    @lombok.SneakyThrows
    void tearDown() {
        if (assertThatExcelFile != null) {
            try {
                assertThatExcelFile.close();
            } catch (AssertionError ignored) {
                //AssertionError is expected as no real data are stored in dummy workbook
            }
            assertThatExcelFile = null;
        }

        if (assertionsFile != null) Files.deleteIfExists(assertionsFile.toPath());
        if (dummyFile != null) Files.deleteIfExists(dummyFile.toPath());
    }

    private static void generateDummyExcelFile(OutputStream output) throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            List<SheetEntry> sheetEntries = new ArrayList<>();

            sheetEntries.add(new SheetEntry("Numbers", List.of()));
            sheetEntries.add(new SheetEntry("StringsFormulas", List.of()));
            sheetEntries.add(new SheetEntry("Strings", List.of()));
            sheetEntries.add(new SheetEntry("Booleans", List.of()));

            ExcelOperations.fillWorkbook(workbook, sheetEntries);
            workbook.write(output);
        }
    }

    private static void generateAssertionsExcelFile(OutputStream output) throws IOException {
        try (var workbook = new XSSFWorkbook()) {

            List<SheetEntry> sheetEntries = new ArrayList<>();

            sheetEntries.add(new SheetEntry("#1", List.of(
                    new NoValueCellEntry("B1", null, "EMPTY")
            )));

            sheetEntries.add(new SheetEntry("Numbers", List.of(
                    new NumberCellEntry("A1", 2.0, "0.00", "="),
                    new NumberCellEntry("A2", 33.0, "0.0000%", ">"),
                    new NumberCellEntry("A3", 10.0, "0.00", ">="),
                    new NumberCellEntry("A4", 0.0000001, "0.00000000", "<"),
                    new NumberCellEntry("A5", -9999999.0, "#,##0", "<="),
                    new NumberCellEntry("A6", 1.414, "0.0000", ">"),
                    new NumberCellEntry("A7", 3.15, "0.0000", "<")
            )));

            sheetEntries.add(new SheetEntry("StringsFormulas", List.of(
                    FormulaCellEntry.ofNoValue("A2", """
                            "Hello "&"World\""""),
                    FormulaCellEntry.ofNoValue("A3", """
                            TEXT(123456,"##0° 00' 00''")"""),

                    FormulaCellEntry.ofNoValue("A5", """
                            "Line1"&CHAR(10)&"Line2\""""),
                    FormulaCellEntry.ofNoValue("A6", """
                            "123" & "456\""""),
                    FormulaCellEntry.ofNoValue("A7", """
                            "=" & "SUM(1,2)"\s""")
            )));

            sheetEntries.add(new SheetEntry("Strings", List.of(
                    new TextCellEntry("A1", "report", null, "containing"),
                    new TextCellEntry("A2", "Hello World", null, "equalTo"),
                    new TextCellEntry("A3", "123456", null, ""), //not comment tag is same as 'equalTo'
                    new TextCellEntry("A4", "\"\"", null, ""),
                    new TextCellEntry("A5", "Line1.*line2", null, "matching"),
                    new TextCellEntry("A6", "^[1-6]{6}$", null, "matching"),
                    new TextCellEntry("A7", """
                            ^=sUm\\(\\d+,\\d+\\)$""", null, "matching")
            )));

            sheetEntries.add(new SheetEntry("Booleans", List.of(
                    new BooleanCellEntry("A1", true),
                    new BooleanCellEntry("A2", true),
                    new BooleanCellEntry("A3", false),
                    new BooleanCellEntry("A4", false),
                    new BooleanCellEntry("A5", false),
                    new BooleanCellEntry("A6", false),
                    new BooleanCellEntry("A7", true)
            )));

            ExcelOperations.fillWorkbook(workbook, sheetEntries);
            workbook.write(output);
        }
    }
}