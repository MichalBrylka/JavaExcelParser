package conditionalFormattingExcel;

public sealed interface FormattableNumber permits FormattableNumber.Currency, FormattableNumber.Fixed, FormattableNumber.Percentage {

    static Fixed ofFixed(double value) {
        return new Fixed(value);
    }

    static Currency ofCurrency(double value, String symbol) {
        return new Currency(value, symbol);
    }

    static Percentage ofRawPercentage(double value) {
        return new Percentage(value);
    }

    static Percentage ofPercent(double percent) {
        return new Percentage(percent / 100.0);
    }


    record Fixed(double value) implements FormattableNumber {
        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    record Percentage(double rawValue /*stored as fraction, e.g. 0.15 for 15%*/) implements FormattableNumber {
        @Override
        public String toString() {
            return (rawValue * 100) + "%";
        }
    }


    record Currency(double value, String symbol) implements FormattableNumber {
        public Currency {
            if (symbol == null || symbol.isBlank())
                throw new IllegalArgumentException("Currency symbol cannot be null or empty.");
        }

        @Override
        public String toString() {
            return "%s %s".formatted(value, symbol);
        }
    }
}
