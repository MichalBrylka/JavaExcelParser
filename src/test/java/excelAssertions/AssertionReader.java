package excelAssertions;

import excelAssertions.io.*;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

public class AssertionReader {
    public static void readFrom(String assertionFilePath, ExcelAssert excelAssert) {readFrom(new File(assertionFilePath), excelAssert);}

    @lombok.SneakyThrows
    public static void readFrom(File assertionFile, ExcelAssert excelAssert) {
        try (var fis = new FileInputStream(assertionFile);
             var workbook = WorkbookFactory.create(fis)
        ) {
            List<SheetEntry> sheetEntries = ExcelOperations.readWorkbook(workbook);

            for (var sheetEntry : sheetEntries) {
                var sheetName = sheetEntry.name();
                var matcher = numberRefSheetPattern.matcher(sheetName);

                if (matcher.find())
                    excelAssert.inSheet(Integer.parseInt(matcher.group("number")));
                else
                    excelAssert.inSheet(sheetName);

                for (var cellEntry : sheetEntry.cells()) {
                    Set<String> tagSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                    if (cellEntry.comment() instanceof String comment)
                        Arrays.stream(comment.split(","))
                                .map(String::trim)
                                .filter(item -> !item.isEmpty()) // Filter out empty strings that might result from trimming or multiple commas
                                .map(String::toLowerCase)
                                .forEach(tagSet::add);

                    CellAssertion<?> cellAssertion = switch (cellEntry) {
                        case TextCellEntry stringEntry ->
                                new TextCellAssertion(cellEntry.address(), createTextAssertionForText(stringEntry.value(), tagSet));
                        case NumberCellEntry numericEntry ->
                                new NumberCellAssertion(cellEntry.address(), createTextAssertionForNumber(numericEntry.value(), tagSet));
                        case BooleanCellEntry booleanEntry ->
                                new BooleanCellAssertion(cellEntry.address(), booleanEntry.value());
                        case DateCellEntry ignored ->
                                throw new IllegalStateException("Date assertions are not supported yet");
                        case ErrorCellEntry errorEntry ->
                                new ErrorTextCellAssertion(cellEntry.address(), new EqualsTextAssertion(errorEntry.value(), true, true));
                        case FormulaCellEntry<?> formulaEntry ->
                                new FormulaTextCellAssertion(cellEntry.address(), new EqualsTextAssertion(formulaEntry.formula(), true, true));
                        case NoValueCellEntry ignored ->
                                tagSet.contains("empty") ? new EmptyCellAssertion(cellEntry.address()) : new SimpleCellAssertion(cellEntry.address());
                    };

                    var expectedFormat = cellEntry.format() instanceof String format &&
                                         !Objects.equals(format, "General") &&
                                         !Objects.equals(format, "@")
                            ? createTextAssertionForFormat(format, tagSet)
                            : null;
                    if (expectedFormat != null)
                        cellAssertion.withFormat(expectedFormat);

                    excelAssert.has(cellAssertion);
                }
            }
        }
    }

    private static @NotNull NumberAssertion createTextAssertionForNumber(@NotNull Double expectedValue, Set<String> tagSet) {
        if (tagSet.contains("="))
            return new EqualToNumberAssertion(expectedValue);
        else if (tagSet.contains(">"))
            return new GreaterThanNumberAssertion(expectedValue);
        else if (tagSet.contains(">="))
            return new GreaterThanOrEqualToNumberAssertion(expectedValue);
        else if (tagSet.contains("<"))
            return new LessThanNumberAssertion(expectedValue);
        else if (tagSet.contains("<="))
            return new LessThanOrEqualToNumberAssertion(expectedValue);
        else
            return new EqualToNumberAssertion(expectedValue);
    }

    private static TextAssertion<?> createTextAssertionForText(@NotNull String value, Set<String> tagSet) {
        if (tagSet.contains("equalTo"))
            return new EqualsTextAssertion(value, true, true);
        else if (tagSet.contains("containing"))
            return new ContainsTextAssertion(value, true);
        else if (tagSet.contains("matching"))
            return new PatternTextAssertion(value, true, true);
        else
            return new EqualsTextAssertion(value, true, true);
    }

    private static TextAssertion<?> createTextAssertionForFormat(@NotNull String format, @NotNull Set<String> tagSet) {
        if (tagSet.contains("format-equalTo"))
            return new EqualsTextAssertion(format, true, true);
        else if (tagSet.contains("format-containing"))
            return new ContainsTextAssertion(format, true);
        else if (tagSet.contains("format-matching"))
            return new PatternTextAssertion(format, true, true);
        else
            return new EqualsTextAssertion(format, true, true);
    }

    private static final Pattern numberRefSheetPattern = Pattern.compile("^#(?<number>\\d+)$");
}



