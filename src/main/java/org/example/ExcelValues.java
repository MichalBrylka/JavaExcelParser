package org.example;

import java.util.List;

record ParsedRow(List<CellValue> cellValues) {
}

sealed interface CellValue permits
        BooleanCellValue, DateCellValue, EmptyCellValue, ErrorCellValue, NumberCellValue, StringCellValue {
}

record StringCellValue(String value) implements CellValue {
}

record NumberCellValue(double value) implements CellValue {
}

record DateCellValue(java.time.LocalDate value) implements CellValue {
}

record BooleanCellValue(boolean value) implements CellValue {
}

record ErrorCellValue(String value) implements CellValue {
}

final class EmptyCellValue implements CellValue {
    private EmptyCellValue() {
    }

    public static final CellValue INSTANCE = new EmptyCellValue();
}