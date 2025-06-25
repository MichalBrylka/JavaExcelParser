package excelAssertions.io;

import org.jetbrains.annotations.NotNull;

public record BooleanCellEntry(
        @NotNull String address,
        @NotNull Boolean value,
        String format,
        String comment
)
        implements CellEntry<Boolean> {

    public BooleanCellEntry(@NotNull String address, @NotNull Boolean value) {
        this(address, value, null);
    }

    public BooleanCellEntry(@NotNull String address, @NotNull Boolean value, String format) {
        this(address, value, format, null);
    }
}
