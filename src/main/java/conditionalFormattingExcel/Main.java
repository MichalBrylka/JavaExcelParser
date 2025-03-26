package conditionalFormattingExcel;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;

import java.io.*;
import java.awt.Desktop;


@Slf4j
public class Main {
    @lombok.SneakyThrows
    public static void main(String[] args) {
        var inputFile = new File(org.example.Main.class.getClassLoader().getResource("ConditionalFormatting.xlsx").getFile());
        if (!inputFile.exists()) {
            log.warn("File not found: {}", inputFile.getAbsolutePath());
            return;
        }

        String outputFileName = "new_" + inputFile.getName();
        File outputFile = new File(inputFile.getParent(), outputFileName);

        FileInputStream fileIn = new FileInputStream(inputFile);
        var workbook = WorkbookFactory.create(fileIn);
        Sheet sheet = workbook.getSheetAt(0);
        DataFormat dataFormat = workbook.createDataFormat();

        CellStyle fixedStyle = workbook.createCellStyle();
        fixedStyle.setDataFormat(dataFormat.getFormat("0.000"));

        CellStyle percentStyle = workbook.createCellStyle();
        percentStyle.setDataFormat(dataFormat.getFormat("0.00%"));

        for (int i = 0; i < 11; i++) {
            Row row = sheet.getRow(i);
            if (row == null) row = sheet.createRow(i);

            Cell cellA = row.getCell(0);
            if (cellA == null) continue;

            String cellValue = cellA.getStringCellValue().trim().toLowerCase();

            Cell cellC = row.createCell(2);
            cellC.setCellValue(i + 1);

            if ("fixed".equals(cellValue)) {
                cellC.setCellStyle(fixedStyle);
            } else if ("percent".equals(cellValue)) {
                cellC.setCellStyle(percentStyle);
            }
        }

        fileIn.close();
        FileOutputStream fileOut = new FileOutputStream(outputFile);
        workbook.write(fileOut);
        fileOut.close();
        workbook.close();

        log.info("Saved as: {}", outputFile.getAbsolutePath());

        // Open new file automatically
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(outputFile);
        } else {
            log.error("Desktop opening not supported.");
        }
    }
}

