package org.example;

import java.math.BigDecimal;

sealed interface Blank {
}

sealed interface ErrorValue {
    String message();
}

sealed interface Value permits BooleanValueBase, CurrencyValueBase, CustomValueBase, DateValueBase, DoubleValueBase, EnumValueBase, IntegerValueBase, SimplyBlank, StringValueBase {
}

final class SimplyBlank implements Value, Blank {
    private SimplyBlank() {
    }

    public static final Value INSTANCE = new SimplyBlank();
}

sealed interface BooleanValueBase extends Value {
}

record BooleanValue(boolean value) implements BooleanValueBase {
}

record BooleanValueError(String message) implements BooleanValueBase, ErrorValue {
}

final class BooleanValueBlank implements BooleanValueBase, Blank {
    private BooleanValueBlank() {
    }

    public static final BooleanValueBase INSTANCE = new BooleanValueBlank();
}


sealed interface IntegerValueBase extends Value {

}

record IntegerValue(int value) implements IntegerValueBase {
}

record IntegerValueError(String message) implements IntegerValueBase, ErrorValue {
}

final class IntegerValueBlank implements IntegerValueBase, Blank {
    private IntegerValueBlank() {
    }

    public static final IntegerValueBase INSTANCE = new IntegerValueBlank();
}


sealed interface DoubleValueBase extends Value {
}

record DoubleValue(double value) implements DoubleValueBase {
}

record DoubleValueError(String message) implements DoubleValueBase, ErrorValue {
}

final class DoubleValueBlank implements DoubleValueBase, Blank {
    private DoubleValueBlank() {
    }

    public static final DoubleValueBase INSTANCE = new DoubleValueBlank();
}


sealed interface DateValueBase extends Value {
}

record DateValue(java.time.LocalDate value) implements DateValueBase {
}

record DateValueError(String message) implements DateValueBase, ErrorValue {
}

final class DateValueBlank implements DateValueBase, Blank {
    private DateValueBlank() {
    }

    public static final DateValueBase INSTANCE = new DateValueBlank();
}


sealed interface CurrencyValueBase extends Value {
}

record CurrencyValue(BigDecimal value) implements CurrencyValueBase {
}

record CurrencyValueError(String message) implements CurrencyValueBase, ErrorValue {
}

final class CurrencyValueBlank implements CurrencyValueBase, Blank {
    private CurrencyValueBlank() {
    }

    public static final CurrencyValueBase INSTANCE = new CurrencyValueBlank();
}


sealed interface StringValueBase extends Value {
}

record StringValue(String value) implements StringValueBase {
}

record StringValueError(String message) implements StringValueBase, ErrorValue {
}

final class StringValueBlank implements StringValueBase, Blank {
    private StringValueBlank() {
    }

    public static final StringValueBase INSTANCE = new StringValueBlank();
}


sealed interface EnumValueBase extends Value {
}

record EnumValue<T extends Enum<?>>(T value) implements EnumValueBase {
}

record EnumValueError(String message) implements EnumValueBase, ErrorValue {
}

final class EnumValueBlank implements EnumValueBase, Blank {
    private EnumValueBlank() {
    }

    public static final EnumValueBase INSTANCE = new EnumValueBlank();
}


sealed interface CustomValueBase extends Value {
}

record CustomValue<T>(T value) implements CustomValueBase {
}

record CustomValueError(String message) implements CustomValueBase, ErrorValue {
}

final class CustomValueBlank implements CustomValueBase, Blank {
    private CustomValueBlank() {
    }

    public static final CustomValueBase INSTANCE = new CustomValueBlank();
}

