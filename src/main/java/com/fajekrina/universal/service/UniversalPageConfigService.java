package com.fajekrina.universal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import com.fajekrina.universal.model.UniversalPageConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Isolated service for Universal SDUI — does NOT reuse PageConfigService.
 * Validates layout against JSON Schema 2020-12 before saving.
 */
@Service
public class UniversalPageConfigService {

    private final UniversalJsonFileUtils jsonFileUtils;
    private final ObjectMapper objectMapper;
    private final Map<String, UniversalPageConfig> cache = new ConcurrentHashMap<>();
    private volatile JsonSchema jsonSchema;

    @Autowired
    public UniversalPageConfigService(UniversalJsonFileUtils jsonFileUtils, ObjectMapper objectMapper) {
        this.jsonFileUtils = jsonFileUtils;
        this.objectMapper = objectMapper;
    }

    private String cacheKey(String merchantId, String pageKey) {
        return "universal::" + merchantId + "::" + pageKey;
    }

    private JsonSchema getSchema() {
        if (jsonSchema != null) return jsonSchema;
        synchronized (this) {
            if (jsonSchema != null) return jsonSchema;
            try {
                JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
                // try classpath schema first
                ClassPathResource res = new ClassPathResource("universal-schema.json");
                if (res.exists()) {
                    try (InputStream is = res.getInputStream()) {
                        jsonSchema = factory.getSchema(is);
                        return jsonSchema;
                    }
                }
                // fallback: inline minimal schema
                String fallback = buildFallbackSchema();
                jsonSchema = factory.getSchema(objectMapper.readTree(fallback));
                return jsonSchema;
            } catch (Exception e) {
                throw new RuntimeException("Failed to load universal JSON schema", e);
            }
        }
    }

    private String buildFallbackSchema() {
        return """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": {
                    "pageId": {"type": "string"},
                    "pageKey": {"type": "string"},
                    "merchantId": {"type": "string"},
                    "status": {"type": "string", "enum": ["DRAFT","PUBLISHED","ARCHIVED"]},
                    "version": {"type": "integer"},
                    "layout": {
                      "type": "object",
                      "properties": {
                        "type": {"type": "string"},
                        "title": {"type": "string"},
                        "subtitle": {"type": "string"},
                        "components": {
                          "type": "array",
                          "items": {"$ref": "#/$defs/component"}
                        }
                      }
                    }
                  },
                  "$defs": {
                    "component": {
                      "type": "object",
                      "required": ["id"],
                      "anyOf": [{"required":["type"]}, {"required":["component"]}],
                      "properties": {
                        "type": {
                          "type": "string",
                          "enum": ["Button","Card","Container","Flex","Grid","Heading","Image","Text","Form","Input","Select","Textarea","Link","Divider","Hero","Space","Toast","Modal"]
                        },
                        "component": {
                          "type": "string",
                          "enum": ["Button","Card","Container","Flex","Grid","Heading","Image","Text","Form","Input","Select","Textarea","Link","Divider","Hero","Space","Toast","Modal"]
                        },
                        "id": {"type": "string"},
                        "parentId": {"type": ["string","null"]},
                        "order": {"type": "integer", "minimum": 0},
                        "props": {"type": "object"},
                        "style": {"type": "object"},
                        "children": {
                          "type": "array",
                          "items": {"$ref": "#/$defs/component"}
                        }
                      },
                      "additionalProperties": true
                    }
                  }
                }
                """;
    }

    public void validateOrThrow(Map<String, Object> layout) {
        if (layout == null) return;
        try {
            Map<String, Object> pageLike = new HashMap<>();
            pageLike.put("pageId", "tmp");
            pageLike.put("pageKey", "tmp");
            pageLike.put("merchantId", "tmp");
            pageLike.put("status", "DRAFT");
            pageLike.put("layout", layout);
            JsonNode pageNode = objectMapper.valueToTree(pageLike);
            Set<ValidationMessage> errors = getSchema().validate(pageNode);
            if (!errors.isEmpty()) {
                List<String> msgs = errors.stream().map(ValidationMessage::getMessage).sorted().toList();
                throw new IllegalArgumentException("JSON Schema validation failed: " + String.join("; ", msgs));
            }
            Object comps = layout.get("components");
            if (comps instanceof List<?> list) {
                for (Object c : list) {
                    if (c instanceof Map<?,?> m) {
                        validateComponentRecursive((Map<String,Object>) m);
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Schema validation error: " + e.getMessage(), e);
        }
    }

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "Button","Card","Container","Flex","Grid","Heading","Image","Text","Form","Input","Select","Divider","Hero","Space","Textarea","Link","Toast","Modal"
    );

    // Registry-based distance validation (mirrors universal-components.json + universal-tokens.json + universal-style-utils.js)
    private static final Set<String> ALLOWED_FLEX_DIRECTIONS = Set.of("row", "column");
    private static final Set<String> ALLOWED_FLEX_JUSTIFY = Set.of("start", "center", "end", "between");

    private void validateComponentRecursive(Map<String,Object> comp) {
        Object t = comp.get("type");
        if (t == null) t = comp.get("component");
        String type = t != null ? t.toString() : null;
        if (type != null && !ALLOWED_TYPES.contains(type)) {
            throw new IllegalArgumentException("Invalid component type: " + type + " — allowed: " + ALLOWED_TYPES);
        }
        // distance / layout standardization checks (same as universal-style-utils.js + universal-components.json)
        if (type != null) {
            Map<String,Object> props = comp.get("props") instanceof Map ? (Map<String,Object>) comp.get("props") : Map.of();
            if ("Grid".equals(type)) {
                Object raw = props.get("columns") != null ? props.get("columns") : props.get("numColumns");
                if (raw != null) {
                    try {
                        int n = Integer.parseInt(raw.toString());
                        if (n < 1 || n > 12) throw new IllegalArgumentException("Grid.columns must be 1-12, got " + raw);
                    } catch (NumberFormatException ex) {
                        throw new IllegalArgumentException("Grid.columns must be integer 1-12, got " + raw);
                    }
                }
            }
            if ("Flex".equals(type)) {
                Object dir = props.get("direction");
                if (dir != null && !ALLOWED_FLEX_DIRECTIONS.contains(dir.toString())) {
                    throw new IllegalArgumentException("Flex.direction must be row|column, got " + dir);
                }
                Object justify = props.get("justify");
                if (justify != null && !ALLOWED_FLEX_JUSTIFY.contains(justify.toString())) {
                    throw new IllegalArgumentException("Flex.justify must be start|center|end|between, got " + justify);
                }
            }
        }
        Object children = comp.get("children");
        if (children instanceof List<?> list) {
            for (Object child : list) {
                if (child instanceof Map<?,?> m) {
                    validateComponentRecursive((Map<String,Object>) m);
                }
            }
        }
    }

    public List<String> listPages(String merchantId) throws IOException {
        return jsonFileUtils.listPages(merchantId);
    }

    public UniversalPageConfig getPage(String merchantId, String pageKey) throws IOException {
        String key = cacheKey(merchantId, pageKey);
        UniversalPageConfig config = jsonFileUtils.readFile(merchantId, pageKey, UniversalPageConfig.class);
        if (config != null) {
            cache.put(key, config);
        } else {
            cache.remove(key);
        }
        return config;
    }

    public UniversalPageConfig savePage(String merchantId, String pageKey, UniversalPageConfig incoming) throws IOException {
        if (incoming != null && incoming.getLayout() != null) {
            validateOrThrow(incoming.getLayout());
        }
        UniversalPageConfig existing = getPage(merchantId, pageKey);
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            existing = new UniversalPageConfig();
            existing.setPageId(merchantId + "-" + pageKey);
            existing.setCreatedAt(now);
            existing.setVersion(1L);
            existing.setStatus("DRAFT");
        } else {
            existing.setVersion((existing.getVersion() == null ? 0L : existing.getVersion()) + 1);
        }
        if (incoming != null && incoming.getLayout() != null) {
            existing.setLayout(incoming.getLayout());
        }
        if (incoming != null && incoming.getStatus() != null && !incoming.getStatus().isBlank()) {
            String s = incoming.getStatus().toUpperCase();
            if (!Set.of("DRAFT","PUBLISHED","ARCHIVED").contains(s)) {
                throw new IllegalArgumentException("Invalid status: " + incoming.getStatus());
            }
            existing.setStatus(s);
        }
        if (incoming != null && incoming.getPageId() != null && !incoming.getPageId().isBlank()) {
            existing.setPageId(incoming.getPageId());
        }
        existing.setMerchantId(merchantId);
        existing.setPageKey(pageKey);
        existing.setUpdatedAt(now);
        jsonFileUtils.writeFile(merchantId, pageKey, existing);
        cache.put(cacheKey(merchantId, pageKey), existing);
        return existing;
    }

    public UniversalPageConfig publishPage(String merchantId, String pageKey) throws IOException {
        UniversalPageConfig config = getPage(merchantId, pageKey);
        if (config == null) return null;
        config.setStatus("PUBLISHED");
        config.setUpdatedAt(LocalDateTime.now());
        jsonFileUtils.writeFile(merchantId, pageKey, config);
        cache.put(cacheKey(merchantId, pageKey), config);
        return config;
    }

    public UniversalPageConfig getPublishedPage(String merchantId, String pageKey) throws IOException {
        UniversalPageConfig config = getPage(merchantId, pageKey);
        if (config == null) return null;
        if (!"PUBLISHED".equalsIgnoreCase(config.getStatus())) return null;
        return config;
    }

    public boolean deletePage(String merchantId, String pageKey) throws IOException {
        cache.remove(cacheKey(merchantId, pageKey));
        return jsonFileUtils.deleteFile(merchantId, pageKey);
    }

    public Map<String, Object> handleAction(String action, Map<String, Object> data, Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        result.put("action", action);
        result.put("receivedAt", LocalDateTime.now().toString());

        if ("submit".equalsIgnoreCase(action) || "SUBMIT_FORM".equalsIgnoreCase(action) || "FORM_SUBMIT".equalsIgnoreCase(action)) {
            // Generic form submit: validate at least one field present
            if (data == null || data.isEmpty()) {
                result.put("success", false);
                result.put("error", "Form data is empty");
                return result;
            }
            result.put("success", true);
            result.put("message", "Form submitted successfully");
            result.put("data", data);
            // navigation target: check params.target or data.target or default to checkout/home
            String target = null;
            if (params != null && params.get("target") != null) target = params.get("target").toString();
            if (target == null && data.get("target") != null) target = data.get("target").toString();
            if (target == null) target = "checkout";
            result.put("target", target);
            result.put("navigateTo", target);
            return result;
        }
        if ("SEAT_SELECTION".equalsIgnoreCase(action) || "selectSeat".equalsIgnoreCase(action)) {
            Object seatId = data != null ? data.get("seatId") : null;
            if (seatId == null || seatId.toString().isBlank()) {
                result.put("success", false);
                result.put("error", "seatId is required");
                return result;
            }
            result.put("success", true);
            result.put("message", "Seat " + seatId + " reserved");
            result.put("target", "checkout");
            result.put("navigateTo", "checkout");
            return result;
        }
        if ("CHECKOUT".equalsIgnoreCase(action)) {
            result.put("success", true);
            result.put("message", "Checkout submitted");
            result.put("data", data);
            result.put("target", "home");
            result.put("navigateTo", "home");
            return result;
        }
        if ("PAYMENT".equalsIgnoreCase(action)) {
            result.put("success", true);
            result.put("message", "Payment processed");
            result.put("target", "home");
            result.put("navigateTo", "home");
            return result;
        }
        result.put("success", true);
        result.put("message", "Action '" + action + "' processed (universal)");
        result.put("data", data);
        result.put("params", params);
        if (params != null && params.get("target") != null) {
            result.put("target", params.get("target"));
            result.put("navigateTo", params.get("target"));
        }
        return result;
    }
}
