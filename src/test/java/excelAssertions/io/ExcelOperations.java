package excelAssertions.io;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;

import java.util.*;

public class ExcelOperations {

    public static List<SheetEntry> readWorkbook(Workbook workbook) {
        List<SheetEntry> sheetEntries = new ArrayList<>();
        for (Sheet sheet : workbook) {
            List<CellEntry<?>> sheetCells = new ArrayList<>();
            for (Row row : sheet) {
                for (Cell cell : row) {
                    String address = cell.getAddress().formatAsString();
                    String comment = (cell.getCellComment() != null) ? cell.getCellComment().getString().getString() : null;

                    String format = cell.getCellStyle().getDataFormatString();

                    CellEntry<?> cellEntry = switch (cell.getCellType()) {
                        case STRING -> new TextCellEntry(address, cell.getStringCellValue(), format, comment);
                        case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                                ? new DateCellEntry(address, cell.getLocalDateTimeCellValue(), format, comment)
                                : new NumberCellEntry(address, cell.getNumericCellValue(), format, comment);

                        case BOOLEAN -> new BooleanCellEntry(address, cell.getBooleanCellValue(), format, comment);

                        case FORMULA -> {
                            CellEntry<?> resultCellEntry = switch (cell.getCachedFormulaResultType()) {
                                case STRING -> new TextCellEntry(address, cell.getStringCellValue(), format, comment);
                                case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                                        ? new DateCellEntry(address, cell.getLocalDateTimeCellValue(), format, comment)
                                        : new NumberCellEntry(address, cell.getNumericCellValue(), format, comment);
                                case BOOLEAN ->
                                        new BooleanCellEntry(address, cell.getBooleanCellValue(), format, comment);
                                case BLANK -> new NoValueCellEntry(address, format, comment);
                                case ERROR ->
                                        new ErrorCellEntry(address, FormulaError.forInt(cell.getErrorCellValue()).getString(), format, comment);

                                default ->
                                        throw new IllegalArgumentException("Formula cell @%s contains content type %s that is not supported".formatted(address, cell.getCachedFormulaResultType()));
                            };
                            yield FormulaCellEntry.ofValue(resultCellEntry, cell.getCellFormula());
                        }
                        case ERROR ->
                                new ErrorCellEntry(address, FormulaError.forInt(cell.getErrorCellValue()).getString(), format, comment);
                        case BLANK -> new NoValueCellEntry(address, format, comment);
                        case _NONE -> null;// Should not happen with typical Excel files, but handles completeness.
                    };
                    if (cellEntry != null)
                        sheetCells.add(cellEntry);
                }
            }
            sheetEntries.add(new SheetEntry(sheet.getSheetName(), sheetCells));
        }
        return sheetEntries;
    }

    public static void fillWorkbook(Workbook workbook, List<SheetEntry> sheetEntries) {
        CreationHelper createHelper = workbook.getCreationHelper();

        for (SheetEntry sheetEntry : sheetEntries) {
            Sheet sheet = workbook.getSheet(sheetEntry.name()) instanceof Sheet s ? s : workbook.createSheet(sheetEntry.name());

            if (sheetEntry.cells() == null || sheetEntry.cells().isEmpty())
                continue;

            for (CellEntry<?> cellEntry : sheetEntry.cells()) {
                if (cellEntry == null) continue;

                CellReference cellRef = new CellReference(cellEntry.address());
                int rowNum = cellRef.getRow();
                int colNum = cellRef.getCol();

                Row row = sheet.getRow(rowNum) instanceof Row r ? r : sheet.createRow(rowNum);

                Cell cell = row.getCell(colNum) instanceof Cell c ? c : row.createCell(colNum);


                switch (cellEntry) {
                    case TextCellEntry stringEntry -> cell.setCellValue(stringEntry.value());
                    case NumberCellEntry numericEntry -> cell.setCellValue(numericEntry.value());
                    case BooleanCellEntry booleanEntry -> cell.setCellValue(booleanEntry.value());
                    case DateCellEntry dateEntry -> cell.setCellValue(dateEntry.value());
                    case ErrorCellEntry errorEntry ->
                            cell.setCellErrorValue(FormulaError.forString(errorEntry.value()).getCode());
                    case FormulaCellEntry<?> formulaEntry -> {
                        String formula = formulaEntry.formula();
                        if (!formula.isEmpty())
                            cell.setCellFormula(formula);
                    }
                    case NoValueCellEntry ignored -> {
                    }
                    default ->
                            throw new IllegalStateException("Unsupported CellEntry type for cell %s: %s".formatted(cellEntry.address(), cellEntry.getClass()));
                }

                if (cellEntry.comment() != null && !cellEntry.comment().isEmpty()) {
                    Drawing<?> drawing = sheet.createDrawingPatriarch();
                    ClientAnchor anchor = createHelper.createClientAnchor();
                    // Anchor comment to the cell itself (or expand as needed)
                    anchor.setCol1(cell.getColumnIndex());
                    anchor.setCol2(cell.getColumnIndex() + 1);
                    anchor.setRow1(cell.getRowIndex());
                    anchor.setRow2(cell.getRowIndex() + 1);

                    Comment comment = drawing.createCellComment(anchor);
                    comment.setString(createHelper.createRichTextString(cellEntry.comment()));
                    cell.setCellComment(comment);
                }


                if (cellEntry.format() != null && !cellEntry.format().isEmpty()) {
                    CellStyle cellStyle = workbook.createCellStyle();
                    cellStyle.setDataFormat(createHelper.createDataFormat().getFormat(cellEntry.format()));
                    cell.setCellStyle(cellStyle);
                }
            }
        }
    }
}
