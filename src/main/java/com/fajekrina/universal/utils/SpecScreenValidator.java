package com.fajekrina.universal.utils;

import com.fajekrina.universal.utils.enums.UniversalFault;
import com.fajekrina.universal.utils.exception.UniversalException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Save/publish-time check of a spec screen payload ({schemaVersion, screen}) against the
 * bundled {@code sdui-spec.schema.json} — the same schema the @fajekrina/sdui client and CI use.
 * The client additionally enforces its runtime bounds when it loads the screen.
 */
public final class SpecScreenValidator {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static volatile JsonSchema schema;

    private SpecScreenValidator() {}

    private static JsonSchema schema() {
        if (schema != null) return schema;
        synchronized (SpecScreenValidator.class) {
            if (schema != null) return schema;
            try (InputStream is = new ClassPathResource("sdui-spec.schema.json").getInputStream()) {
                schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(is);
                return schema;
            } catch (Exception e) {
                throw new IllegalStateException("Failed to load the SDUI spec schema", e);
            }
        }
    }

    /**
     * Validates {@code payload} and that its {@code screen.key} equals {@code screenKey}.
     * Returns the payload as a map; throws {@code INVALID_SCREEN} with the violations.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> validate(Object payload, String screenKey) {
        if (!(payload instanceof Map<?, ?> map)) throw new UniversalException(UniversalFault.INVALID_SCREEN, "payload must be a JSON object");
        JsonNode node = MAPPER.valueToTree(map);
        Set<ValidationMessage> errors = schema().validate(node);
        if (!errors.isEmpty()) {
            List<String> messages = errors.stream().map(ValidationMessage::getMessage).sorted().limit(5).toList();
            throw new UniversalException(UniversalFault.INVALID_SCREEN, String.join("; ", messages));
        }
        String key = node.path("screen").path("key").asText();
        if (!key.equals(screenKey)) {
            throw new UniversalException(UniversalFault.INVALID_SCREEN, "screen.key \"" + key + "\" does not match the path key \"" + screenKey + "\"");
        }
        return (Map<String, Object>) map;
    }
}
