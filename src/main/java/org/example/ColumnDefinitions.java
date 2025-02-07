package org.example;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.*;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.poi.ss.usermodel.DateUtil;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/*List<ColumnDefinition> dataTypes = Arrays.asList(
                IntegerColumnDefinition.INSTANCE,
                new DoubleColumnDefinition("#.###"),
                new DoubleColumnDefinition("#.##########"),
                EmptyColumnDefinition.INSTANCE,
                CurrencyColumnDefinition.INSTANCE,
                new DateColumnDefinition("dd.MM.yyyy"),
                new StringColumnDefinition(),
                new EnumColumnDefinition(Color.class),
                new CustomColumnDefinition(Price.class),
                new StringColumnDefinition()
        );


        var objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        String json = objectMapper.writeValueAsString(dataTypes);


        List<ColumnDefinition> deser = objectMapper.readValue(json, new TypeReference<>() {
        });*/
@JsonSerialize(using = ColumnDefinitionSerializer.class)
@JsonDeserialize(using = ColumnDefinitionDeserializer.class)
sealed interface ColumnDefinition<TValue extends Value> permits
        BooleanColumnDefinition, CurrencyColumnDefinition, CustomColumnDefinition,
        DateColumnDefinition, DoubleColumnDefinition, EmptyColumnDefinition,
        EnumColumnDefinition, IntegerColumnDefinition, StringColumnDefinition {
    TValue getValue(CellValue cellValue);
}

final class ColumnDefinitionCommons {
    static final String KIND = "kind";
    static final String FORMAT = "format";
    static final String TYPE = "type";

    //TODO add tests to check that against reflected state
    static final Map<Class<? extends ColumnDefinition<? extends Value>>, String> COLUMN_DEFINITION_TYPE_MAP = Map.of(
            BooleanColumnDefinition.class, "boolean",
            EmptyColumnDefinition.class, "empty",
            IntegerColumnDefinition.class, "integer",
            CurrencyColumnDefinition.class, "currency",

            DateColumnDefinition.class, "date",
            DoubleColumnDefinition.class, "double",
            StringColumnDefinition.class, "string",

            CustomColumnDefinition.class, "custom",
            EnumColumnDefinition.class, "enum"
    );

    static final Map<String, Class<?>> CUSTOM_TYPE_TYPE_MAP = Map.of(
            "Price", Price.class
    );

    static final Map<String, Class<? extends Enum<?>>> ENUM_TYPE_TYPE_MAP = Map.of(
            "Color", Color.class,
            "Size", Size.class
    );
}

final class ColumnDefinitionSerializer extends JsonSerializer<ColumnDefinition<? extends Value>> {
    @Override
    public void serialize(ColumnDefinition<? extends Value> columnDefinition, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (columnDefinition == null) {
            gen.writeNull();
            return;
        }
        gen.writeStartObject();

        var kind = ColumnDefinitionCommons.COLUMN_DEFINITION_TYPE_MAP.get(columnDefinition.getClass());
        if (kind == null)
            throw new IllegalStateException(columnDefinition.getClass().getSimpleName() + " is not supported");
        gen.writeStringField(ColumnDefinitionCommons.KIND, kind);

        switch (columnDefinition) {
            case BooleanColumnDefinition ignored:
                break; //nothing to serialize except kind
            case EmptyColumnDefinition ignored:
                break; //nothing to serialize except kind
            case IntegerColumnDefinition ignored:
                break; //nothing to serialize except kind
            case CurrencyColumnDefinition ignored:
                break; //nothing to serialize except kind

            case DateColumnDefinition(var format):
                writeFormat(gen, format);
                break;
            case DoubleColumnDefinition(var format):
                writeFormat(gen, format);
                break;
            case StringColumnDefinition(var format):
                writeFormat(gen, format);
                break;

            case EnumColumnDefinition(var enumType):
                writeClass(gen, enumType, ColumnDefinitionCommons.ENUM_TYPE_TYPE_MAP);
                break;
            case CustomColumnDefinition(var customType):
                writeClass(gen, customType, ColumnDefinitionCommons.CUSTOM_TYPE_TYPE_MAP);
                break;

            default:
                throw new IllegalStateException(columnDefinition.getClass().getSimpleName() + " is not supported");
        }
        gen.writeEndObject();
    }

    private static void writeFormat(JsonGenerator gen, String format) throws IOException {
        gen.writeStringField(ColumnDefinitionCommons.FORMAT, format);
    }

    private static void writeClass(JsonGenerator gen, Class<?> clazz, Map<String, ?> map) throws IOException {
        var typeName = map.entrySet().stream().filter(kvp -> clazz.equals(kvp.getValue())).findFirst().orElseThrow(() -> new IllegalStateException(clazz.getSimpleName() + " is not supported")).getKey();
        gen.writeStringField(ColumnDefinitionCommons.TYPE, typeName);
    }
}

final class ColumnDefinitionDeserializer extends JsonDeserializer<ColumnDefinition<? extends Value>> {
    @Override
    public ColumnDefinition<? extends Value> deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
        TreeNode node = p.getCodec().readTree(p);

        if (node.isObject() && node.fieldNames() instanceof Iterator<String> fields) {
            if (!fields.hasNext())
                return EmptyColumnDefinition.INSTANCE;

            if (!(node.get(ColumnDefinitionCommons.KIND) instanceof TextNode kindNode) || kindNode.asText().isEmpty())
                throw new JsonParseException(p, "kind field is obligatory for non-empty nodes");
            String kind = kindNode.asText();

            return switch (kind.toLowerCase()) {
                case "boolean" -> checkOnlyKindAndReturn(p, node, BooleanColumnDefinition.INSTANCE);
                case "empty" -> checkOnlyKindAndReturn(p, node, EmptyColumnDefinition.INSTANCE);
                case "integer" -> checkOnlyKindAndReturn(p, node, IntegerColumnDefinition.INSTANCE);
                case "currency" -> checkOnlyKindAndReturn(p, node, CurrencyColumnDefinition.INSTANCE);

                case "date" ->
                        checkKindAndFormatAtMost(p, node) instanceof String format ? new DateColumnDefinition(format) : new DateColumnDefinition();
                case "double" ->
                        checkKindAndFormatAtMost(p, node) instanceof String format ? new DoubleColumnDefinition(format) : new DoubleColumnDefinition();
                case "string" ->
                        checkKindAndFormatAtMost(p, node) instanceof String format ? new StringColumnDefinition(format) : new StringColumnDefinition();

                case "custom" -> new CustomColumnDefinition(checkKindAndType(p, node));
                case "enum" -> new EnumColumnDefinition(checkKindAndTypeForEnum(p, node));
                default -> throw new IllegalStateException(kind + " is not supported");
            };
        } else throw new JsonParseException(p, """
                Invalid schema for ColumnDefinition. Supported schema are:
                {} -> empty column definition
                {"kind": "boolean|empty|integer|currency"}
                {"kind": "date|double|string", "format": "optional format"}
                {"kind": "custom|enum", "type": "type definition"}
                """);
    }

    private static ColumnDefinition<? extends Value> checkOnlyKindAndReturn(JsonParser p, TreeNode node, ColumnDefinition<? extends Value> instance) throws JsonParseException {
        var fieldIterator = node.fieldNames();

        while (fieldIterator.hasNext()) {
            var field = fieldIterator.next();
            if (!ColumnDefinitionCommons.KIND.equals(field))
                throw new JsonParseException(p, "'kind' is only supported field for boolean|empty|integer|currency");
        }
        return instance;
    }

    private static String checkKindAndFormatAtMost(JsonParser p, TreeNode node) throws JsonParseException {
        var fieldIterator = node.fieldNames();

        String format = null;
        while (fieldIterator.hasNext()) {
            var field = fieldIterator.next();
            if (!ColumnDefinitionCommons.KIND.equals(field) && !ColumnDefinitionCommons.FORMAT.equals(field))
                throw new JsonParseException(p, "'kind' and 'format' are only supported field for date|double|string");
            if (ColumnDefinitionCommons.FORMAT.equals(field) && node.get(ColumnDefinitionCommons.FORMAT) instanceof TextNode formatNode)
                format = formatNode.asText();
        }
        return format;
    }

    private static Class<?> checkKindAndType(JsonParser p, TreeNode node) throws JsonParseException {
        var fieldIterator = node.fieldNames();

        Class<?> type = null;
        while (fieldIterator.hasNext()) {
            var field = fieldIterator.next();
            if (!ColumnDefinitionCommons.KIND.equals(field) && !ColumnDefinitionCommons.TYPE.equals(field))
                throw new JsonParseException(p, "'kind' and 'type' are only supported field for custom");
            if (ColumnDefinitionCommons.TYPE.equals(field) && node.get(ColumnDefinitionCommons.TYPE) instanceof TextNode typeNode)
                type = ColumnDefinitionCommons.CUSTOM_TYPE_TYPE_MAP.get(typeNode.asText());
        }
        if (type == null)
            throw new JsonParseException(p, "'type' is obligatory field for custom. Supported types are: " + String.join(", ", ColumnDefinitionCommons.CUSTOM_TYPE_TYPE_MAP.keySet()));
        return type;
    }

    private static Class<? extends Enum<?>> checkKindAndTypeForEnum(JsonParser p, TreeNode node) throws JsonParseException {
        var fieldIterator = node.fieldNames();

        Class<? extends Enum<?>> type = null;
        while (fieldIterator.hasNext()) {
            var field = fieldIterator.next();
            if (!ColumnDefinitionCommons.KIND.equals(field) && !ColumnDefinitionCommons.TYPE.equals(field))
                throw new JsonParseException(p, "'kind' and 'type' are only supported field for enum");
            if (ColumnDefinitionCommons.TYPE.equals(field) && node.get(ColumnDefinitionCommons.TYPE) instanceof TextNode typeNode)
                type = ColumnDefinitionCommons.ENUM_TYPE_TYPE_MAP.get(typeNode.asText());
        }
        if (type == null)
            throw new JsonParseException(p, "'type' is obligatory field for enum. Supported types are: " + String.join(", ", ColumnDefinitionCommons.ENUM_TYPE_TYPE_MAP.keySet()));
        return type;
    }
}


final class EmptyColumnDefinition implements ColumnDefinition<Value> {
    private EmptyColumnDefinition() {
    }

    public static final ColumnDefinition<Value> INSTANCE = new EmptyColumnDefinition();

    @Override
    public Value getValue(CellValue cellValue) {
        return SimplyBlank.INSTANCE;
    }
}

final class BooleanColumnDefinition implements ColumnDefinition<BooleanValueBase> {
    private BooleanColumnDefinition() {
    }

    @SuppressWarnings("unused")
    public static final ColumnDefinition<BooleanValueBase> INSTANCE = new BooleanColumnDefinition();

    @Override
    public BooleanValueBase getValue(CellValue cellValue) {
        return switch (cellValue) {
            case NumberCellValue(var num) -> new BooleanValue(num != 0);
            case StringCellValue(var text) -> new BooleanValue(text != null && !text.isEmpty());
            case EmptyCellValue ignored -> BooleanValueBlank.INSTANCE;
            case ErrorCellValue(var err) -> new BooleanValueError(err);
            case BooleanCellValue(var b) -> new BooleanValue(b);
            case DateCellValue(var date) -> new BooleanValue(date != LocalDate.MIN);
        };
    }
}

final class IntegerColumnDefinition implements ColumnDefinition<IntegerValueBase> {
    private IntegerColumnDefinition() {
    }

    public static final ColumnDefinition<IntegerValueBase> INSTANCE = new IntegerColumnDefinition();

    @Override
    public IntegerValueBase getValue(CellValue cellValue) {
        return switch (cellValue) {
            case NumberCellValue(var num) -> new IntegerValue((int) Math.round(num));
            case StringCellValue(var text) -> {
                try {
                    yield new IntegerValue(Integer.parseInt(text));
                } catch (NumberFormatException e) {
                    yield new IntegerValueError("String value is not an integer: " + text);
                }
            }
            case EmptyCellValue ignored -> IntegerValueBlank.INSTANCE;
            case ErrorCellValue(var err) -> new IntegerValueError(err);
            case BooleanCellValue(var b) -> new IntegerValue(b ? 1 : 0);
            case DateCellValue(var date) -> new IntegerValue((int) Math.floor(DateUtil.getExcelDate(date)));
        };
    }
}

@SuppressWarnings("unused")
record DoubleColumnDefinition(String format) implements ColumnDefinition<DoubleValueBase> {
    DoubleColumnDefinition() {
        this("#.##");
    }

    @Override
    public DoubleValueBase getValue(CellValue cellValue) {
        return switch (cellValue) {
            case NumberCellValue(var num) -> new DoubleValue(num);
            case StringCellValue(var text) -> {
                try {
                    var df = (DecimalFormat) NumberFormat.getNumberInstance(Locale.US);
                    if (format instanceof String f && !f.isEmpty())
                        df.applyPattern(f);
                    yield new DoubleValue(df.parse(text).doubleValue());
                } catch (Exception e) {
                    yield new DoubleValueError("String value is not valid number: " + text + " under format '" + format + "'");
                }
            }
            case EmptyCellValue ignored -> DoubleValueBlank.INSTANCE;
            case ErrorCellValue(var err) -> new DoubleValueError(err);
            case BooleanCellValue(var b) -> new DoubleValue(b ? 1.0 : 0.0);
            case DateCellValue(var date) -> new DoubleValue(DateUtil.getExcelDate(date));
        };
    }
}

record DateColumnDefinition(String format) implements ColumnDefinition<DateValueBase> {
    @SuppressWarnings("unused")
    DateColumnDefinition() {
        this("yyyy/MM/dd");
    }

    @Override
    public DateValueBase getValue(CellValue cellValue) {
        return switch (cellValue) {
            case NumberCellValue(double num) -> new DateValue(DateUtil.getLocalDateTime(num).toLocalDate());
            case StringCellValue(String text) -> {
                var f = format != null && !format.isEmpty() ? format : "yyyy/MM/dd";
                try {
                    var formatter = DateTimeFormatter.ofPattern(f).withLocale(Locale.US);

                    yield new DateValue(LocalDate.parse(text, formatter));
                } catch (Exception e) {
                    yield new DateValueError("Date cannot be parsed from: " + text + " with format: '" + f + "'");
                }
            }
            case EmptyCellValue ignored -> DateValueBlank.INSTANCE;
            case ErrorCellValue(var err) -> new DateValueError(err);
            case BooleanCellValue(boolean value) -> new DateValue(value ? LocalDate.MAX : LocalDate.MIN);
            case DateCellValue(var date) -> new DateValue(date);
        };
    }

}

final class CurrencyColumnDefinition implements ColumnDefinition<CurrencyValueBase> {
    private CurrencyColumnDefinition() {
    }

    public static final ColumnDefinition<CurrencyValueBase> INSTANCE = new CurrencyColumnDefinition();

    @Override
    public CurrencyValueBase getValue(CellValue cellValue) {
        return switch (cellValue) {
            case NumberCellValue(var num) -> new CurrencyValue(new BigDecimal(num));
            case StringCellValue(var text) -> {
                try {
                    var df = (DecimalFormat) NumberFormat.getNumberInstance(Locale.US);
                    df.setParseBigDecimal(true);

                    yield new CurrencyValue((BigDecimal) df.parseObject(text));
                } catch (Exception e) {
                    yield new CurrencyValueError("String value is not valid number: " + text);
                }
            }
            case EmptyCellValue ignored -> CurrencyValueBlank.INSTANCE;
            case ErrorCellValue(var err) -> new CurrencyValueError(err);
            case BooleanCellValue(var b) -> new CurrencyValue(b ? BigDecimal.ONE : BigDecimal.ZERO);
            case DateCellValue(var date) -> new CurrencyValue(BigDecimal.valueOf(DateUtil.getExcelDate(date)));
        };
    }
}

record StringColumnDefinition(String format) implements ColumnDefinition<StringValueBase> {
    StringColumnDefinition() {
        this("");
    }

    public StringValueBase getValue(CellValue cellValue) {
        return switch (cellValue) {
            case NumberCellValue(var num) -> {
                try {
                    var df = (DecimalFormat) NumberFormat.getNumberInstance(Locale.US);
                    if (format instanceof String f && !f.isEmpty())
                        df.applyPattern(f);
                    yield new StringValue(df.format(num));
                } catch (Exception e) {
                    yield new StringValueError("String cannot be formatted from number: " + num);
                }
            }
            case StringCellValue(var text) ->
                    text == null || text.isEmpty() ? StringValueBlank.INSTANCE : new StringValue(text);
            case EmptyCellValue ignored -> StringValueBlank.INSTANCE;
            case ErrorCellValue(var err) -> new StringValueError(err);
            case BooleanCellValue(var b) -> new StringValue(b ? "TRUE" : "FALSE");
            case DateCellValue(var date) -> {
                var f = format != null && !format.isEmpty() ? format : "yyyy/MM/dd";
                try {
                    var formatter = DateTimeFormatter.ofPattern(f).withLocale(Locale.US);

                    yield new StringValue(formatter.format(date));
                } catch (Exception e) {
                    yield new StringValueError("String cannot be formatted from date: " + date + " with format: '" + f + "'");
                }
            }
        };
    }
}

record EnumColumnDefinition(Class<? extends Enum<?>> enumType) implements ColumnDefinition<EnumValueBase> {
    private static final Map<Class<? extends Enum<?>>, Function<String, ? extends Enum<?>>> enumParsers = Map.of(
            Color.class, Color::fromName,
            Size.class, Size::fromName
    );

    public EnumValueBase getValue(CellValue cellValue) {
        return switch (cellValue) {
            case StringCellValue(String text) -> {
                var enumParser = enumParsers.get(enumType);
                if (enumParser == null)
                    yield new EnumValueError("Not supported enumeration: " + enumType.getSimpleName());

                var enumValue = enumParser.apply(text);
                yield enumValue != null ? new EnumValue<>(enumValue) : new EnumValueError(enumType.getSimpleName() + " cannot be parsed from:" + text);
            }
            case EmptyCellValue ignored -> EnumValueBlank.INSTANCE;
            case ErrorCellValue(var err) -> new EnumValueError(err);
            default -> new EnumValueError("Enum value cannot be obtained");
        };
    }
}

record CustomColumnDefinition(Class<?> customType) implements ColumnDefinition<CustomValueBase> {
    private static final Map<Class<?>, Function<String, ?>> textParsers = Map.of(
            Price.class, Price::parse
    );

    private static final Map<Class<?>, Function<Double, ?>> numberParsers = Map.of(
            Price.class, Price::of
    );

    public CustomValueBase getValue(CellValue cellValue) {
        return switch (cellValue) {
            case StringCellValue(String text) -> {
                var textParser = textParsers.get(customType);
                if (textParser == null)
                    yield new CustomValueError("Custom type is not supported for text parsing: " + customType.getSimpleName());

                try {
                    var parsed = textParser.apply(text);
                    yield parsed == null ? new CustomValueError(customType.getSimpleName() + " cannot be parsed from: " + text) : new CustomValue<>(parsed);
                } catch (Exception e) {
                    yield new CustomValueError(customType.getSimpleName() + " cannot be parsed from: " + text + " due to: " + e.getMessage());
                }
            }
            case NumberCellValue(double num) -> {
                var numParser = numberParsers.get(customType);
                if (numParser == null)
                    yield new CustomValueError("Custom type is not supported for number conversion: " + customType.getSimpleName());

                try {
                    var converted = numParser.apply(num);
                    yield converted == null ? new CustomValueError(customType.getSimpleName() + " cannot be converted from: " + num) : new CustomValue<>(converted);
                } catch (Exception e) {
                    yield new CustomValueError(customType.getSimpleName() + " cannot be converted from: " + num + " due to: " + e.getMessage());
                }
            }
            case EmptyCellValue ignored -> CustomValueBlank.INSTANCE;
            case ErrorCellValue(var err) -> new CustomValueError(err);
            default -> new CustomValueError("Custom value cannot be obtained");
        };
    }
}