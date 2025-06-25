package excelAssertions.io;

import org.jetbrains.annotations.NotNull;

public record NumericCellEntry(
        @NotNull String address,
        @NotNull Double value,
        String format,
        String comment)
        implements CellEntry<Double> {

    public NumericCellEntry(@NotNull String address, @NotNull Double value) {
        this(address, value, null);
    }

    public NumericCellEntry(@NotNull String address, @NotNull Double value, String format) {
        this(address, value, format, null);
    }
}
