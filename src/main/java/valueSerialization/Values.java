package valueSerialization;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.time.*;

@JsonSerialize(using = ValueSerializer.class)
@JsonDeserialize(using = ValueDeserializer.class)
sealed interface Value permits Blank,
        BooleanValue, CurrencyValue, DateValue, DoubleValue, ErrorValue, IntegerValue, LongValue, StringValue, TimeValue,
        CustomValue, EnumValue {
    @NotNull ValueDiscriminator getDiscriminator();
}

enum ValueDiscriminator {
    BLANK("blank"),
    BOOLEAN("boolean"), CURRENCY("currency"), DATE("date"), DOUBLE("double"), ERROR("error"), INTEGER("integer"), LONG("long"), STRING("string"), TIME("time"),
    CUSTOM("custom"), ENUM("enum");

    private final @NotNull String name;

    public @NotNull String getName() {
        return name;
    }

    ValueDiscriminator(@NotNull String name) {
        this.name = name;
    }

    public static ValueDiscriminator fromName(String text) {
        return text == null || text.isEmpty() ? null :
                switch (text.trim().toLowerCase()) {
                    case "blank" -> BLANK;

                    case "boolean" -> BOOLEAN;
                    case "currency" -> CURRENCY;
                    case "date" -> DATE;
                    case "double" -> DOUBLE;
                    case "error" -> ERROR;
                    case "integer" -> INTEGER;
                    case "long" -> LONG;
                    case "string" -> STRING;
                    case "time" -> TIME;

                    case "custom" -> CUSTOM;
                    case "enum" -> ENUM;
                    default -> null;
                };
    }
}

@lombok.NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
final class Blank implements Value {
    public static final Value BLANK = new Blank();

    @Override
    public String toString() {
        return "";
    }

    @Override
    public @NotNull ValueDiscriminator getDiscriminator() {
        return ValueDiscriminator.BLANK;
    }
}

record BooleanValue(boolean value) implements Value {
    public static final BooleanValue TRUE = new BooleanValue(true);
    public static final BooleanValue FALSE = new BooleanValue(false);

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public @NotNull ValueDiscriminator getDiscriminator() {
        return ValueDiscriminator.BOOLEAN;
    }
}

record CurrencyValue(@NotNull BigDecimal value) implements Value {
    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public @NotNull ValueDiscriminator getDiscriminator() {
        return ValueDiscriminator.CURRENCY;
    }
}

record CustomValue<T>(@NotNull T value) implements Value {
    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public @NotNull ValueDiscriminator getDiscriminator() {
        return ValueDiscriminator.CUSTOM;
    }
}

record DateValue(@NotNull LocalDateTime value) implements Value {
    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public @NotNull ValueDiscriminator getDiscriminator() {
        return ValueDiscriminator.DATE;
    }
}

record DoubleValue(double value) implements Value {
    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public @NotNull ValueDiscriminator getDiscriminator() {
        return ValueDiscriminator.DOUBLE;
    }
}

record EnumValue<T extends Enum<?>>(@NotNull T value) implements Value {
    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public @NotNull ValueDiscriminator getDiscriminator() {
        return ValueDiscriminator.ENUM;
    }
}

record ErrorValue(@NotNull String message) implements Value {
    @Override
    public String toString() {
        return "ERROR:" + message;
    }

    @Override
    public @NotNull ValueDiscriminator getDiscriminator() {
        return ValueDiscriminator.ERROR;
    }
}

record IntegerValue(int value) implements Value {
    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public @NotNull ValueDiscriminator getDiscriminator() {
        return ValueDiscriminator.INTEGER;
    }
}

record LongValue(long value) implements Value {
    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public @NotNull ValueDiscriminator getDiscriminator() {
        return ValueDiscriminator.LONG;
    }
}

record StringValue(@NotNull String value) implements Value {
    @Override
    public String toString() {
        return value;
    }

    @Override
    public @NotNull ValueDiscriminator getDiscriminator() {
        return ValueDiscriminator.STRING;
    }
}

record TimeValue(@NotNull LocalTime value) implements Value {
    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public @NotNull ValueDiscriminator getDiscriminator() {
        return ValueDiscriminator.TIME;
    }
}
