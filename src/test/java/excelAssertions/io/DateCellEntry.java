package excelAssertions.io;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;

public record DateCellEntry(
        @NotNull String address,
        @NotNull LocalDateTime value,
        String format,
        String comment
)
        implements CellEntry<LocalDateTime> {

    public DateCellEntry(@NotNull String address, @NotNull LocalDateTime value) {
        this(address, value, null);
    }

    public DateCellEntry(@NotNull String address, @NotNull LocalDateTime value, String format) {
        this(address, value, format, null);
    }
}
