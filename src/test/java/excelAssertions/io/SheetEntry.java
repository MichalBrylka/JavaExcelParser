package excelAssertions.io;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public record SheetEntry(
        @NotNull String name,
        List<CellEntry<?>> cells) {
}

