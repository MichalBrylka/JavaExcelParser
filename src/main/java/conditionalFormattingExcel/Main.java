package conditionalFormattingExcel;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.awt.Desktop;
import java.util.Map;


@Slf4j
public class Main {
    @lombok.SneakyThrows
    public static void main(String[] args) {
        var tempFile = new File(System.getProperty("java.io.tmpdir"), "NumberFormats.xlsx");

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Formats");

        NumberFormat numberFormat = new NumberFormat(4, 1, 2);

        writeFormattedCell(sheet, numberFormat, new CellAddress("A1"), FormattableNumber.ofCurrency(1234.56789, "$"));
        writeFormattedCell(sheet, numberFormat, new CellAddress("A2"), FormattableNumber.ofCurrency(11234.56789, "$"));
        writeFormattedCell(sheet, numberFormat, new CellAddress("A3"), FormattableNumber.ofCurrency(21234.56789, "$"));
        writeFormattedCell(sheet, numberFormat, new CellAddress("A4"), FormattableNumber.ofCurrency(31234.56789, "$"));
        writeFormattedCell(sheet, numberFormat, new CellAddress("A5"), FormattableNumber.ofCurrency(41234.56789, "$"));


        writeFormattedCell(sheet, numberFormat, new CellAddress("B1"), FormattableNumber.ofPercentRaw(0.875));
        writeFormattedCell(sheet, numberFormat, new CellAddress("C1"), FormattableNumber.ofFixed(3.1415926));
        writeFormattedCell(sheet, numberFormat, new CellAddress("D1"), FormattableNumber.ofCurrency(1234.56789, "PLN"));

        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            workbook.write(out);
        }
        workbook.close();

        if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(tempFile);
        else log.error("Desktop opening not supported.");

        printNonEmptyCellsWithFormat(tempFile);
    }

    static void writeFormattedCell(Sheet sheet, NumberFormat numberFormat, CellAddress address, FormattableNumber number) {
        Workbook workbook = sheet.getWorkbook();

        // Create or get row
        Row row = sheet.getRow(address.getRow());
        if (row == null) row = sheet.createRow(address.getRow());

        // Create or get cell
        Cell cell = row.getCell(address.getColumn());
        if (cell == null) cell = row.createCell(address.getColumn());

        // Write value
        cell.setCellValue(number.rawNumber());

        // Create data format
        DataFormat dataFormat = workbook.createDataFormat();
        CellStyle cellStyle = workbook.createCellStyle();


        String formatString = getPoiFormatString(number, numberFormat);


        cellStyle.setDataFormat(dataFormat.getFormat(formatString));
        cell.setCellStyle(cellStyle);
    }

    private static String getPoiFormatString(FormattableNumber number, NumberFormat numberFormat) {
        return switch (number) {
            case FormattableNumber.Fixed ignored -> {
                var sb = new StringBuilder();
                sb.append("0");
                if (numberFormat.fixedDecimalPlaces() > 0)
                    sb.append(".").append("0".repeat(numberFormat.fixedDecimalPlaces()));
                yield sb.toString();
            }

            case FormattableNumber.Percentage ignored -> {
                var sb = new StringBuilder();
                sb.append("0");
                if (numberFormat.percentDecimalPlaces() > 0)
                    sb.append(".").append("0".repeat(numberFormat.percentDecimalPlaces()));
                sb.append("%");
                yield sb.toString();
            }

            case FormattableNumber.Currency(var ignored, String currencySymbol) -> {
                var sb = new StringBuilder();
                currencySymbol = currencySymbol.replace("\"", "\"\"");
                boolean shouldAppend = shouldAppendCurrencySymbol(currencySymbol);

                if (!shouldAppend)
                    sb.append("\"").append(currencySymbol).append(" \"");

                sb.append("#,##0");
                if (numberFormat.currencyDecimalPlaces() > 0)
                    sb.append(".").append("0".repeat(numberFormat.currencyDecimalPlaces()));

                if (shouldAppend)
                    sb.append(" \"").append(currencySymbol).append("\"");

                yield sb.toString();
            }
        };

    }

    private static boolean shouldAppendCurrencySymbol(String symbol) {
        symbol = symbol.trim();
        return "zł".equals(symbol) || "PLN".equals(symbol);
        // add other symbols here potentially adding locale parameter to this method
        // For the Euro (EUR), symbol placement varies by country.
        // In English-speaking countries, it's typically placed before the amount (e.g., €1,234.56),
        // while in other European countries, it may appear after the amount with a non-breaking space (e.g., 1.234,56 €)
    }

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

