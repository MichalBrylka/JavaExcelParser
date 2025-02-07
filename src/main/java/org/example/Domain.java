package org.example;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

enum Color {
    RED("red"), GREEN("green"), BLUE("blue");
    private final String name;

    Color(String name) {
        this.name = name;
    }

    public static Color fromName(String text) {
        return text == null || text.isEmpty() ? null :
                switch (text.trim().toLowerCase()) {
                    case "red" -> RED;
                    case "green" -> GREEN;
                    case "blue" -> BLUE;
                    default -> null;
                };
    }


    @Override
    public String toString() {
        return name;
    }
}

enum Size {
    S("s"), M("m"), L("l"), XL("xl");
    private final String name;

    Size(String name) {
        this.name = name;
    }

    public static Size fromName(String text) {
        return text == null || text.isEmpty() ? null :
                switch (text.trim().toLowerCase()) {
                    case "s" -> S;
                    case "m" -> M;
                    case "l" -> L;
                    case "xl" -> XL;
                    default -> null;
                };
    }

    @Override
    public String toString() {
        return name;
    }
}

@lombok.EqualsAndHashCode
final class Price {
    private final Double value;
    private final boolean isMkt;

    private Price(Double value) {
        this.value = value;
        this.isMkt = false;
    }

    private Price() {
        this.value = null;
        this.isMkt = true;
    }

    public static Price of(Double value) {
        return new Price(value);
    }

    public static Price mkt() {
        return new Price();
    }

    public static Price parse(String text) {
        if (text == null || text.isEmpty())
            throw new IllegalArgumentException("Input string cannot be null.");

        if (containsOnlyConsecutiveMkt(text)) return mkt();

        try {
            return of(NumberFormat.getNumberInstance(Locale.US).parse(text).doubleValue());

        } catch (ParseException e) {
            throw new IllegalArgumentException("Only MKT or valid number is allowed in: " + text, e);
        }
    }

    private static boolean containsOnlyConsecutiveMkt(String str) {
        if (str == null) {
            return false;
        }

        int state = 0; // 0: initial/whitespace, 1: m, 2: mk, 3: mkt, 4: trailing whitespace

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);

            switch (state) {
                case 0: // Initial/Whitespace
                    if (Character.isWhitespace(c)) {
                        continue; // Stay in whitespace state
                    } else if (Character.toLowerCase(c) == 'm') {
                        state = 1;
                    } else {
                        return false; // Invalid character
                    }
                    break;
                case 1: // 'm' seen
                    if (Character.toLowerCase(c) == 'k') {
                        state = 2;
                    } else {
                        return false; // Not 'mk'
                    }
                    break;
                case 2: // 'mk' seen
                    if (Character.toLowerCase(c) == 't') {
                        state = 3;
                    } else {
                        return false; // Not 'mkt'
                    }
                    break;
                case 3: // 'mkt' seen
                    if (Character.toLowerCase(c) == 'm') {
                        state = 1; // Start of next 'mkt'
                    } else if (Character.isWhitespace(c)) {
                        state = 4; // Trailing whitespace allowed
                    } else {
                        return false; // Invalid character
                    }
                    break;
                case 4: // Trailing whitespace
                    if (!Character.isWhitespace(c)) {
                        return false; // No more non-whitespace allowed
                    }
                    break;
            }
        }

        return state == 3 || state == 4; // Valid states at the end
    }

    public boolean isMkt() {
        return isMkt;
    }

    public boolean isNumber() {
        return !isMkt;
    }

    public Double getNumber() {
        if (isMkt) {
            throw new IllegalStateException("Cannot get number from an MKT value.");
        }
        return value;
    }


    @Override
    public String toString() {
        return isMkt ? "MKT" : String.valueOf(value);
    }
}