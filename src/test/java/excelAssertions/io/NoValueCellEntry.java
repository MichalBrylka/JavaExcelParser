package excelAssertions.io;

import org.jetbrains.annotations.NotNull;


public record NoValueCellEntry(
        @NotNull String address,
        String format,
        String comment
)
        implements CellEntry<Object> {
    @Override
    public Object value() {return null;}

    public NoValueCellEntry(@NotNull String address) {
        this(address, null);
    }

    public NoValueCellEntry(@NotNull String address, String format) {
        this(address, format, null);
    }
}
