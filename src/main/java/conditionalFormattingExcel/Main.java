package conditionalFormattingExcel;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.awt.Desktop;


@Slf4j
public class Main {
    @lombok.SneakyThrows
    public static void main(String[] args) {
        var tempFile = new File(System.getProperty("java.io.tmpdir"), "NumberFormats.xlsx");

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Formats");

     /*   Format formatUsd = new Format.CurrencyFormat(2, "$");
        Format formatPercent = new Format.PercentFormat(1);
        Format formatDecimal = new Format.FixedFormat(8);

        writeFormattedCell(sheet, formatUsd, new CellAddress("A1"), 1234.56);
        writeFormattedCell(sheet, formatPercent, new CellAddress("B1"), 0.875);
        writeFormattedCell(sheet, formatDecimal, new CellAddress("C1"), 3.1415926);*/

        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            workbook.write(out);
        }
        workbook.close();

        if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(tempFile);
        else log.error("Desktop opening not supported.");

        printNonEmptyCellsWithFormat(tempFile);
    }

    /*static void writeFormattedCell(Sheet sheet, Format format, CellAddress address, double value) {
        Workbook workbook = sheet.getWorkbook();

        // Create or get row
        Row row = sheet.getRow(address.getRow());
        if (row == null) {
            row = sheet.createRow(address.getRow());
        }

        // Create or get cell
        Cell cell = row.getCell(address.getColumn());
        if (cell == null) {
            cell = row.createCell(address.getColumn());
        }

        // Write value
        cell.setCellValue(value);

        // Create data format
        DataFormat dataFormat = workbook.createDataFormat();
        CellStyle cellStyle = workbook.createCellStyle();

        StringBuilder formatString = new StringBuilder();

        if (format instanceof Format.PercentFormat(var decimalPlaces)) {
            formatString.append("0");
            if (decimalPlaces > 0) {
                formatString.append(".");
                formatString.append("0".repeat(decimalPlaces));
            }
            formatString.append("%");
        } else if (format instanceof Format.CurrencyFormat(
                var decimalPlaces, var symbol
        ) && symbol != null && !symbol.isEmpty()) {
            formatString.append("\"").append(symbol).append("\"");
            formatString.append("#,##0");

            if (decimalPlaces > 0) {
                formatString.append(".");
                formatString.append("0".repeat(decimalPlaces));
            }
        } else if (format instanceof Format.FixedFormat(var decimalPlaces)) {
            formatString.append("0");
            if (decimalPlaces > 0) {
                formatString.append(".");
                formatString.append("0".repeat(decimalPlaces));
            }
        }

        cellStyle.setDataFormat(dataFormat.getFormat(formatString.toString()));
        cell.setCellStyle(cellStyle);
    }*/

    @lombok.SneakyThrows
    static void printNonEmptyCellsWithFormat(File excelFile) {
        try (FileInputStream fis = new FileInputStream(excelFile);
             Workbook workbook = new XSSFWorkbook(fis)) {

            for (Sheet sheet : workbook) {
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        if (cell.getCellType() == CellType.BLANK) continue;

                        // Cell address
                        String address = cell.getAddress().formatAsString();

                        // Raw cell value
                        String value = getRawValue(cell);

                        // Format string
                        CellStyle style = cell.getCellStyle();
                        short formatIndex = style.getDataFormat();
                        String format = style != null ? style.getDataFormatString() : "(none)";

                        System.out.printf("Cell %s: value = %s, format = %s, formatIndex =%s%n ", address, value, format, formatIndex);
                    }
                }
            }
        }
    }

    private static String getRawValue(Cell cell) {
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return Double.toString(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return Boolean.toString(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case ERROR:
                return Byte.toString(cell.getErrorCellValue());
            default:
                return "(unknown)";
        }
    }


}

