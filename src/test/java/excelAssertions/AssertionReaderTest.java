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
import static org.assertj.core.api.Assertions.*;

class AssertionReaderTest {
    private ExcelAssert assertThatExcelFile;
    private File assertionsFile;
    private File dummyFile;

    @Test
    void readFrom_ShouldReadAssertionsFromExcel() {
        usingNewExcelFile();
        AssertionReader.readFrom(assertionsFile, assertThatExcelFile);
        var actualAssertions = assertThatExcelFile.getAssertions();

        var expectedAssertions = Stream.of(
                new ExpAss(1, new EmptyCellAssertion("B1")),

                new ExpAss("Numbers", new NumberCellAssertion("A1", new EqualToNumberAssertion(2.0)).withFormat(new EqualsTextAssertion("0.00", true, true))),
                new ExpAss("Numbers", new NumberCellAssertion("A2", new GreaterThanNumberAssertion(33.0)).withFormat(new EqualsTextAssertion("0.0000%", true, true))),
                new ExpAss("Numbers", new NumberCellAssertion("A3", new GreaterThanOrEqualToNumberAssertion(10.0)).withFormat(new EqualsTextAssertion("0.00", true, true))),
                new ExpAss("Numbers", new NumberCellAssertion("A4", new LessThanNumberAssertion(0.0000001)).withFormat(new EqualsTextAssertion("0.00000000", true, true))),
                new ExpAss("Numbers", new NumberCellAssertion("A5", new LessThanOrEqualToNumberAssertion(-9999999.0)).withFormat(new EqualsTextAssertion("#,##0", true, true))),
                new ExpAss("Numbers", new NumberCellAssertion("A6", new GreaterThanNumberAssertion(1.414)).withFormat(new EqualsTextAssertion("0.0000", true, true))),
                new ExpAss("Numbers", new NumberCellAssertion("A7", new LessThanNumberAssertion(3.15)).withFormat(new EqualsTextAssertion("0.0000", true, true))),

                new ExpAss("StringsFormulas", new FormulaTextCellAssertion("A2", new EqualsTextAssertion("""
                        "Hello "&"World\"""", true, true))),
                new ExpAss("StringsFormulas", new FormulaTextCellAssertion("A3", new EqualsTextAssertion("""
                        TEXT(123456,"##0° 00' 00''")""", true, true))),
                new ExpAss("StringsFormulas", new FormulaTextCellAssertion("A5", new EqualsTextAssertion("""
                        "Line1"&CHAR(10)&"Line2\"""", true, true))),
                new ExpAss("StringsFormulas", new FormulaTextCellAssertion("A6", new EqualsTextAssertion("""
                        "123" & "456\"""", true, true))),
                new ExpAss("StringsFormulas", new FormulaTextCellAssertion("A7", new EqualsTextAssertion("""
                        "=" & "SUM(1,2)"\s""", true, true))),

                new ExpAss("Strings", new TextCellAssertion("A1", new ContainsTextAssertion("report", true))),
                new ExpAss("Strings", new TextCellAssertion("A2", new EqualsTextAssertion("Hello World", true, true))),
                new ExpAss("Strings", new TextCellAssertion("A3", new EqualsTextAssertion("123456", true, true))),
                new ExpAss("Strings", new TextCellAssertion("A4", new EqualsTextAssertion("\"\"", true, true))),
                new ExpAss("Strings", new TextCellAssertion("A5", new PatternTextAssertion("Line1.*line2", true, true))),
                new ExpAss("Strings", new TextCellAssertion("A6", new PatternTextAssertion("^[1-6]{6}$", true, true))),
                new ExpAss("Strings", new TextCellAssertion("A7", new PatternTextAssertion("""
                        ^=sUm\\(\\d+,\\d+\\)$""", true, true))),

                new ExpAss("Booleans", new BooleanCellAssertion("A1", true)),
                new ExpAss("Booleans", new BooleanCellAssertion("A2", true)),
                new ExpAss("Booleans", new BooleanCellAssertion("A3", false)),
                new ExpAss("Booleans", new BooleanCellAssertion("A4", false)),
                new ExpAss("Booleans", new BooleanCellAssertion("A5", false)),
                new ExpAss("Booleans", new BooleanCellAssertion("A6", false)),
                new ExpAss("Booleans", new BooleanCellAssertion("A7", true))
        ).map(
                ea -> new ExcelAssert.CellAssertionAtSheet(
                        ea.assertion.withSheetName(ea.sheetRef instanceof String s ? s : "StringsFormulas"),
                        switch (ea.sheetRef) {
                            case String s -> new ExcelAssert.SheetRefByName(s);
                            case Integer i -> new ExcelAssert.SheetRefByIndex(i);
                            default -> throw new IllegalStateException(ea.sheetRef + " is not supported");
                        }
                )
        ).toList();

        assertThat(actualAssertions)
                .usingRecursiveComparison()
                .isEqualTo(expectedAssertions);
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
                    new TextCellEntry("A3", "123456", null, ""), //no comment tag is same as 'equalTo'
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