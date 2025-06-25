package excelAssertions;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

public class AssertionReader {
    @lombok.SneakyThrows
    public static void readFrom(File assertionFile, ExcelAssert excelAssert) {
        try (var fis = new FileInputStream(assertionFile);
             var workbook = WorkbookFactory.create(fis)
        ) {

        }
    }

    public static void readFrom(String assertionFilePath, ExcelAssert excelAssert) {readFrom(new File(assertionFilePath), excelAssert);}


}



