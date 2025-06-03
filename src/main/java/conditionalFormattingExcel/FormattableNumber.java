package conditionalFormattingExcel;

import org.jetbrains.annotations.NotNull;

public sealed interface FormattableNumber permits FormattableNumber.Currency, FormattableNumber.Fixed, FormattableNumber.Percentage {
    double rawNumber();

    static Fixed ofFixed(double value) {
        return new Fixed(value);
    }

    static Percentage ofPercentRaw(double value) {
        return new Percentage(value);
    }

    static Percentage ofPercent(double percent) {
        return new Percentage(percent / 100.0);
    }

    static Currency ofCurrency(double value, String symbol) {
        return new Currency(value, symbol);
    }

    default FormattableNumber withRawNumber(double rawNumber) {
        return switch (this) {
            case Fixed ignored -> ofFixed(rawNumber);
            case Percentage ignored -> ofPercentRaw(rawNumber);
            case Currency(var ignored, String currencySymbol) -> ofCurrency(rawNumber, currencySymbol);
        };
    }

    record Fixed(double rawNumber) implements FormattableNumber {
        @Override
        public String toString() {
            return String.valueOf(rawNumber);
        }
    }

    record Percentage(double rawNumber /*stored as fraction, e.g. 0.15 for 15%*/) implements FormattableNumber {
        @Override
        public String toString() {
            return (rawNumber * 100) + "%";
        }
    }


    record Currency(double rawNumber, @NotNull String symbol) implements FormattableNumber {
        public Currency {
            if (symbol == null || symbol.isBlank())
                throw new IllegalArgumentException("Currency symbol cannot be null or empty.");
        }

        @Override
        public String toString() {
            return "%s %s".formatted(rawNumber, symbol);
        }
    }
}
