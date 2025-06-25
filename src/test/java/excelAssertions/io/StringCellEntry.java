package excelAssertions.io;

import org.jetbrains.annotations.NotNull;

public record StringCellEntry(
        @NotNull String address,
        @NotNull String value,
        String format,
        String comment
)
        implements CellEntry<String> {

    public StringCellEntry(@NotNull String address, @NotNull String value) {
        this(address, value, null);
    }

    public StringCellEntry(@NotNull String address, @NotNull String value, String format) {
        this(address, value, format, null);
    }
}
