package base64Enc;

import com.fasterxml.jackson.databind.annotation.*;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.IOException;

@JsonSerialize(using = EncodedString.EncodedStringSerializer.class)
@JsonDeserialize(using = EncodedString.EncodedStringDeserializer.class)
public record EncodedString(String text) {

    private static final String PREFIX = "$base64:";
    private static final Pattern BASE64_PATTERN = Pattern.compile("""
            ^\\$base64:(.*?)\\$$""", Pattern.DOTALL);

    public static String encode(String text) {
        String encoded = Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
        return PREFIX + encoded + "$";
    }

    public static String decode(String text) {
        Matcher matcher = BASE64_PATTERN.matcher(text);
        if (matcher.matches()) {
            String base64 = matcher.group(1);
            return new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
        }
        return text;
    }

    static class EncodedStringSerializer extends JsonSerializer<EncodedString> {
        @Override
        public void serialize(EncodedString value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value == null || value.text() == null)
                gen.writeNull();
            else
                gen.writeString(encode(value.text()));
        }
    }

    static class EncodedStringDeserializer extends JsonDeserializer<EncodedString> {
        @Override
        public EncodedString deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            String raw = p.getText();
            return new EncodedString(raw == null ? null : decode(raw));
        }
    }
}


