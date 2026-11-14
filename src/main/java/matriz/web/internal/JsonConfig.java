package matriz.web.internal;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.type.LogicalType;

/**
 * Strict reading of request bodies (DoD07, api-contract.md AS07): unknown fields and fields of the wrong
 * JSON type are refused with {@code 400}, instead of being ignored or converted (e.g. the number 123
 * where text is expected). Text containing the NUL character is refused the same way: PostgreSQL can't
 * store it, so it would otherwise end in a server error (code-security-review.md, F1).
 */
@Configuration
class JsonConfig {

    @Bean
    JsonMapperBuilderCustomizer strictRequestBodies() {
        return builder -> builder
                .addModule(new SimpleModule("text-without-nul").addDeserializer(String.class, new TextWithoutNul()))
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .withCoercionConfig(LogicalType.Textual, text -> text
                        .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail))
                .withCoercionConfig(LogicalType.Integer, number -> number
                        .setCoercion(CoercionInputShape.String, CoercionAction.Fail))
                .withCoercionConfig(LogicalType.Float, number -> number
                        .setCoercion(CoercionInputShape.String, CoercionAction.Fail))
                .withCoercionConfig(LogicalType.Boolean, bool -> bool
                        .setCoercion(CoercionInputShape.String, CoercionAction.Fail));
    }

    /** A JSON string, refused if it contains NUL; anything that isn't a string is refused as before. */
    static final class TextWithoutNul extends StdDeserializer<String> {

        TextWithoutNul() {
            super(String.class);
        }

        @Override
        public String deserialize(JsonParser parser, DeserializationContext context) {
            if (!parser.hasToken(JsonToken.VALUE_STRING)) {
                return (String) context.handleUnexpectedToken(String.class, parser);
            }
            String text = parser.getString();
            if (text.indexOf('\u0000') >= 0) {
                return context.reportInputMismatch(this, "Text must not contain the NUL character");
            }
            return text;
        }
    }
}
