package excelAssertions.io;

import org.jetbrains.annotations.NotNull;

public final class FormulaCellEntry<T> implements CellEntry<T> {
    private final @NotNull String address;
    private final @NotNull String formula;
    private final T value;
    private final String format;
    private final String comment;

    private FormulaCellEntry(@NotNull String address, @NotNull String formula, T value, String format, String comment) {
        this.address = address;
        this.formula = formula;
        this.value = value;
        this.format = format;
        this.comment = comment;
    }

    public static <T> FormulaCellEntry<T> ofValue(@NotNull CellEntry<T> resultCell, @NotNull String formula) {
        return new FormulaCellEntry<T>(resultCell.address(), formula, resultCell.value(), resultCell.format(), resultCell.comment());
    }

    public static FormulaCellEntry<Object> ofNoValue(@NotNull String address, @NotNull String formula) {return ofNoValue(address, formula, null);}

    public static FormulaCellEntry<Object> ofNoValue(@NotNull String address, @NotNull String formula, String format) {return ofNoValue(address, formula, format, null);}

    public static FormulaCellEntry<Object> ofNoValue(@NotNull String address, @NotNull String formula, String format, String comment) {
        return new FormulaCellEntry<Object>(address, formula, null, format, comment);
    }

    @Override
    public @NotNull String address() {return address;}

    public @NotNull String formula() {return formula;}

    @Override
    public T value() {return value;}

    @Override
    public String format() {return format;}

    @Override
    public String comment() {return comment;}
}