package enumParser;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
public class Main {
    @SneakyThrows
    public static void main(String[] args) {
        log.info("Parser: {}", Fruits.from("banana"));
    }
}

interface NamedEnum {
    @NotNull String getName();

    static <T extends Enum<T> & NamedEnum> T fromName(Class<T> enumClass, String name) {
        return fromName(enumClass, name, false);
    }

    static <T extends Enum<T> & NamedEnum> T fromName(Class<T> enumClass, String name, boolean caseSensitive) {
        for (T constant : enumClass.getEnumConstants()) {
            String fieldValue = constant.getName();
            if (caseSensitive ? fieldValue.equals(name) : fieldValue.equalsIgnoreCase(name)) {
                return constant;
            }
        }
        String allowedValues = Arrays.stream(enumClass.getEnumConstants())
                .map(NamedEnum::getName)
                .collect(Collectors.joining(", "));
        throw new IllegalArgumentException("No enum constant in %s with name: %s. Allowed values: %s".formatted(enumClass.getSimpleName(), name, allowedValues));
    }
}

@Getter
@RequiredArgsConstructor
enum Size implements NamedEnum {
    S("s"), M("m"), L("l"), XL("xl");

    private final @NotNull String name;

    public static Size from(String text) {
        return NamedEnum.fromName(Size.class, text);
    }
}

@Getter
@RequiredArgsConstructor
enum Fruits implements NamedEnum {
    APPLE("apple"), BANANA("banana"), CHERRY("cherry"), DATE("date");

    private final @NotNull String name;

    public static Fruits from(String text) {
        return NamedEnum.fromName(Fruits.class, text);
    }
}

