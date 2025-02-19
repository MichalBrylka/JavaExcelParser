package valueSerialization;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

public class TypeMetaConformityTests {
    @ParameterizedTest
    @ValueSource(classes = {Color.class, Size.class, Price.class})
    void testActualDomainClasses(Class<?> clazz) {
        assertThat(hasValidFromMethod(clazz))
                .describedAs(clazz.getSimpleName() + " should have a valid 'from' method")
                .isEqualTo(true);

        assertThat(hasOverriddenToString(clazz))
                .describedAs(clazz.getSimpleName() + " should override 'toString'")
                .isEqualTo(true);
    }



    @ParameterizedTest(name = "{index} => class={0}, expectedFromMethod={1}, expectedToString={2}")
    @MethodSource("provideTestClasses")
    void testProvingLogic(Class<?> clazz, boolean expectedFromMethod, boolean expectedToString) {
        assertThat(hasValidFromMethod(clazz))
                .describedAs(clazz.getSimpleName() + " should have a valid 'from' method")
                .isEqualTo(expectedFromMethod);

        assertThat(hasOverriddenToString(clazz))
                .describedAs(clazz.getSimpleName() + " should override 'toString'")
                .isEqualTo(expectedToString);
    }

    // MethodSource that provides test cases (valid and invalid classes)
    static Stream<Arguments> provideTestClasses() {
        return Stream.of(
                // ✅ Positive Cases
                Arguments.of(Named.of("ExampleClass", ExampleClass.class), true, true),
                Arguments.of(Named.of("StringFromClass", StringFromClass.class), true, true),

                // ❌ Negative Cases
                Arguments.of(Named.of("NoFromMethodClass", NoFromMethodClass.class), false, true),
                Arguments.of(Named.of("InheritedToStringClass", InheritedToStringClass.class), true, false),
                Arguments.of(Named.of("CompletelyInvalidClass", CompletelyInvalidClass.class), false, false)
        );
    }

    // Method to check if the class has a static "from" method with 1 parameter (Object or String)
    static boolean hasValidFromMethod(Class<?> clazz) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers()) &&
                    method.getName().equals("from") &&
                    method.getParameterCount() == 1 &&
                    (method.getParameterTypes()[0] == Object.class || method.getParameterTypes()[0] == String.class)) {
                return true;
            }
        }
        return false;
    }

    // Method to check if the class overrides toString() at its level
    static boolean hasOverriddenToString(Class<?> clazz) {
        try {
            Method toStringMethod = clazz.getDeclaredMethod("toString");
            return toStringMethod.getDeclaringClass() == clazz; // Must be declared in clazz itself
        } catch (NoSuchMethodException e) {
            return false;
        }
    }


    // ✅ Valid class: has static from(Object) + toString()
    static class ExampleClass {
        public static ExampleClass from(Object obj) { return new ExampleClass(); }
        @Override public String toString() { return "ExampleClass"; }
    }

    // ✅ Valid class: has static from(String) + toString()
    static class StringFromClass {
        public static StringFromClass from(String str) { return new StringFromClass(); }
        @Override public String toString() { return "StringFromClass"; }
    }

    // ❌ Invalid: No "from" method, but overrides toString()
    static class NoFromMethodClass {
        @Override public String toString() { return "NoFromMethodClass"; }
    }

    // ❌ Invalid: Has static "from(Object)" but does NOT override toString()
    static class InheritedToStringClass {
        public static InheritedToStringClass from(Object obj) { return new InheritedToStringClass(); }
        // No toString() (uses Object's)
    }

    // ❌ Completely invalid: No "from" method, No toString()
    static class CompletelyInvalidClass {}
}
