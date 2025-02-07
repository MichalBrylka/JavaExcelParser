package org.example;

import org.apache.poi.ss.usermodel.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;


public class Main {
    public static void main(String[] args) throws IOException {
        List<ColumnDefinition> dataTypes = Arrays.asList(
                IntegerColumnDefinition.INSTANCE,
                new DoubleColumnDefinition("#.###"),
                new DoubleColumnDefinition("#.##########"),
                EmptyColumnDefinition.INSTANCE,
                CurrencyColumnDefinition.INSTANCE,
                new DateColumnDefinition("dd.MM.yyyy"),
                new StringColumnDefinition(),
                new EnumColumnDefinition(Color.class),
                new CustomColumnDefinition(Price.class),
                new StringColumnDefinition()
        );

        List<ParsedRow> rows;
        try (var fileStream = Main.class.getClassLoader().getResourceAsStream("Parsing.xlsx")) {
            rows = readExcel(fileStream);
        }

        var headerTypes = Collections.nCopies(10, new StringColumnDefinition());

        var header = parseValues(rows.getFirst(), headerTypes).stream().map(h -> ((StringValue) h).value()).toList();
        var data = rows.stream().skip(1).map(row -> parseValues(row, dataTypes)).toList();
    }

    private static List<Value> parseValues(ParsedRow parsedRow, List<? extends ColumnDefinition> columnDefinitions) {
        var cellValues = parsedRow.cellValues();

        if (cellValues.size() != columnDefinitions.size()) throw new IllegalStateException("Meta size do not match");

        var result = new ArrayList<Value>();

        for (int i = 0; i < cellValues.size(); i++) {
            var cellValue = cellValues.get(i);
            Value value = columnDefinitions.get(i).getValue(cellValue);
            result.add(value);
        }

        return result;
    }

    static List<ParsedRow> readExcel(InputStream inputStream) {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            var parsedRows = new ArrayList<ParsedRow>(sheet.getLastRowNum());

            for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row dataRow = sheet.getRow(rowIndex);
                if (isRowEmpty(dataRow)) continue;

                var cellValues = new ArrayList<CellValue>(dataRow.getLastCellNum());
                for (int colIndex = 0; colIndex < dataRow.getLastCellNum(); colIndex++) {
                    cellValues.add(getCellValue(dataRow.getCell(colIndex)));
                }

                parsedRows.add(new ParsedRow(cellValues));
            }
            return parsedRows;
        } catch (IOException e) {
            System.err.println("Error reading the Excel file: " + e.getMessage());
            return null;
        }
    }

    private static CellValue getCellValue(Cell cell) {
        if (cell == null) return EmptyCellValue.INSTANCE;

        return switch (cell.getCellType()) {
            case STRING -> new StringCellValue(cell.getStringCellValue());
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? new DateCellValue(cell.getLocalDateTimeCellValue().toLocalDate())
                    : new NumberCellValue(cell.getNumericCellValue());
            case BOOLEAN -> new BooleanCellValue(cell.getBooleanCellValue());
            case BLANK, _NONE -> EmptyCellValue.INSTANCE;
            case ERROR ->
                    new ErrorCellValue(FormulaError.forInt(cell.getErrorCellValue()).getString() + "@" + cell.getAddress());

            case FORMULA -> switch (cell.getCachedFormulaResultType()) {
                case STRING -> new StringCellValue(cell.getStringCellValue());
                case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                        ? new DateCellValue(cell.getLocalDateTimeCellValue().toLocalDate())
                        : new NumberCellValue(cell.getNumericCellValue());
                case BOOLEAN -> new BooleanCellValue(cell.getBooleanCellValue());
                case BLANK -> EmptyCellValue.INSTANCE;
                case ERROR ->
                        new ErrorCellValue(FormulaError.forInt(cell.getErrorCellValue()).getString() + "@" + cell.getAddress() + ":" + cell.getCellFormula());
                default -> new ErrorCellValue("Cannot determine formula at cell " + cell.getAddress());
            };
        };
    }


    private static boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }

        for (Cell cell : row) {
            if (!isCellEmpty(cell)) {
                return false; // Found a non-empty cell, row is not empty
            }
        }

        return true; // All cells are empty, row is empty
    }

    private static boolean isCellEmpty(Cell cell) {
        if (cell == null) {
            return true;
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().isEmpty();
            case NUMERIC, BOOLEAN:
                return false;
            case FORMULA:
                try {
                    String formulaResult = cell.getCellFormula();
                    return formulaResult == null || formulaResult.isEmpty();
                } catch (Exception e) {
                    return true;
                }
            case BLANK, ERROR, _NONE:
            default:
                return true;
        }
    }
}

