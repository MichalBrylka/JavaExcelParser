package valueSerialization;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.stream.Collectors;

public class EnumHelper {
    public static <T extends Enum<T>> T fromName(Class<T> enumClass, String name) {
        return fromName(enumClass, name, false);
    }

    public static <T extends Enum<T>> T fromName(Class<T> enumClass, String name, boolean caseSensitive) {
        Field nameField;
        try {
            nameField = enumClass.getDeclaredField("name");
            nameField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("Enum " + enumClass.getSimpleName() + " does not have a 'name' field", e);
        }

        for (T constant : enumClass.getEnumConstants()) {
            try {
                String fieldValue = (String) nameField.get(constant);
                if (caseSensitive ? fieldValue.equals(name) : fieldValue.equalsIgnoreCase(name)) {
                    return constant;
                }
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException("Enum " + enumClass.getSimpleName() + " does not have a 'name' field", e);
            }
        }
        String allowedValues = Arrays.stream(enumClass.getEnumConstants())
                .map(constant -> {
                    try {
                        return (String) nameField.get(constant);
                    } catch (IllegalAccessException e) {
                        return "[error retrieving]";
                    }
                })
                .collect(Collectors.joining(", "));
        throw new IllegalArgumentException("No enum constant in " + enumClass.getSimpleName() + " with name: " + name + ". Allowed values: " + allowedValues);
    }
}
