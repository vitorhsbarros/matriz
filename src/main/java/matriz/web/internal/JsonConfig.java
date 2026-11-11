package matriz.web.internal;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.type.LogicalType;

/**
 * Strict reading of request bodies (DoD07, api-contract.md AS07): unknown fields and fields of the wrong
 * JSON type are refused with {@code 400}, instead of being ignored or converted (e.g. the number 123
 * where text is expected).
 */
@Configuration
class JsonConfig {

    @Bean
    JsonMapperBuilderCustomizer strictRequestBodies() {
        return builder -> builder
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
}
