package excelAssertions.io;

import org.jetbrains.annotations.NotNull;

public record TextCellEntry(
        @NotNull String address,
        @NotNull String value,
        String format,
        String comment
)
        implements CellEntry<String> {

    public TextCellEntry(@NotNull String address, @NotNull String value) {
        this(address, value, null);
    }

    public TextCellEntry(@NotNull String address, @NotNull String value, String format) {
        this(address, value, format, null);
    }
}
