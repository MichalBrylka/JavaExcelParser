package excelAssertions.io;

import org.jetbrains.annotations.NotNull;

public sealed interface CellEntry<T> permits
        BooleanCellEntry, DateCellEntry, ErrorCellEntry, FormulaCellEntry,
        NoValueCellEntry, NumericCellEntry, StringCellEntry {

    @NotNull String address();

    T value();

    String format();

    String comment();
}
