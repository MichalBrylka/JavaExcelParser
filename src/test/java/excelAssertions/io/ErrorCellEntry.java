package excelAssertions.io;

import org.jetbrains.annotations.NotNull;

public record ErrorCellEntry(
        @NotNull String address,
        @NotNull String value,
        String format,
        String comment
)
        implements CellEntry<String> {

    public ErrorCellEntry(@NotNull String address, @NotNull String value) {
        this(address, value, null);
    }

    public ErrorCellEntry(@NotNull String address, @NotNull String value, String format) {
        this(address, value, format, null);
    }
}
