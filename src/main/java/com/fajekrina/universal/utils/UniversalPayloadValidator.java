package com.fajekrina.universal.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import com.fajekrina.universal.utils.enums.UniversalFault;
import com.fajekrina.universal.utils.exception.UniversalException;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.net.URI;
import com.fajekrina.universal.model.dto.ValidationBounds;
import com.fajekrina.universal.model.dto.ComponentRegistry;

import java.util.*;

/**
 * Save-time validation (pure, no I/O besides the bundled JSON schema).
 * Rules come from data, not constants: bounds (sdui_bounds), component types and
 * their allowed behaviors (sdui_component_type), behavior vocabulary (sdui_behavior).
 * Mirrors the client validator in @fajekrina/sdui core/validate.ts.
 */
public class UniversalPayloadValidator {


    private static final Set<String> FLEX_DIRECTIONS = Set.of("row", "column");
    private static final Set<String> FLEX_JUSTIFY = Set.of("start", "center", "end", "between");
    private static volatile JsonSchema schema;

    private final ValidationBounds bounds;
    private final ComponentRegistry registry;

    public UniversalPayloadValidator(ValidationBounds bounds, ComponentRegistry registry) {
        this.bounds = bounds;
        this.registry = registry;
    }

    private static JsonSchema schema() {
        if (schema != null) return schema;
        synchronized (UniversalPayloadValidator.class) {
            if (schema != null) return schema;
            try (InputStream is = new ClassPathResource("universal-schema.json").getInputStream()) {
                schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(is);
                return schema;
            } catch (Exception e) {
                throw new IllegalStateException("Failed to load universal JSON schema", e);
            }
        }
    }

    /** Throws UniversalException(INVALID_PAYLOAD) with a readable message on the first violation. */
    @SuppressWarnings("unchecked")
    public void validateOrThrow(Map<String, Object> layout, Map<String, Object> policy) {
        if (layout == null) return;
        Map<String, Object> pageLike = new HashMap<>();
        pageLike.put("pageId", "tmp");
        pageLike.put("pageKey", "tmp");
        pageLike.put("merchantId", "tmp");
        pageLike.put("status", "DRAFT");
        pageLike.put("layout", layout);
        JsonNode node = new ObjectMapper().valueToTree(pageLike);
        Set<ValidationMessage> errors = schema().validate(node);
        if (!errors.isEmpty()) {
            List<String> msgs = errors.stream().map(ValidationMessage::getMessage).sorted().toList();
            throw new UniversalException(UniversalFault.INVALID_PAYLOAD, "JSON Schema validation failed: " + String.join("; ", msgs));
        }
        Object comps = layout.get("components");
        if (!(comps instanceof List<?> list)) return;
        int[] count = new int[]{0};
        boolean flat = list.stream().anyMatch(c -> c instanceof Map<?, ?> m && m.get("parentId") != null);
        if (flat) {
            // flat parentId/order payload: depth is the parent chain length
            Map<String, Map<String, Object>> byId = new HashMap<>();
            for (Object c : list) if (c instanceof Map<?, ?> m) byId.put(String.valueOf(m.get("id")), (Map<String, Object>) m);
            Map<String, Integer> depths = new HashMap<>();
            for (Map<String, Object> c : byId.values()) {
                int depth = depthOf(String.valueOf(c.get("id")), byId, depths, new HashSet<>());
                validateComponent(c, depth, count, policy);
            }
            Map<String, Integer> childCount = new HashMap<>();
            for (Map<String, Object> c : byId.values()) {
                Object pid = c.get("parentId");
                if (pid != null) childCount.merge(pid.toString(), 1, Integer::sum);
            }
            for (Map.Entry<String, Integer> e : childCount.entrySet()) {
                if (e.getValue() > bounds.maxChildren()) throw new UniversalException(UniversalFault.INVALID_PAYLOAD, "children exceed " + bounds.maxChildren());
                Map<String, Object> parent = byId.get(e.getKey());
                if (parent != null) requireContainer(parent);
            }
        } else {
            for (Object c : list) if (c instanceof Map<?, ?> m) walkNested((Map<String, Object>) m, 1, count, policy);
        }
    }

    private int depthOf(String id, Map<String, Map<String, Object>> byId, Map<String, Integer> memo, Set<String> path) {
        Integer known = memo.get(id);
        if (known != null) return known;
        if (!path.add(id)) throw new UniversalException(UniversalFault.INVALID_PAYLOAD, "parent cycle at component " + id);
        Map<String, Object> c = byId.get(id);
        Object pid = c == null ? null : c.get("parentId");
        int d = pid == null || !byId.containsKey(pid.toString()) ? 1 : depthOf(pid.toString(), byId, memo, path) + 1;
        memo.put(id, d);
        return d;
    }

    @SuppressWarnings("unchecked")
    private void walkNested(Map<String, Object> comp, int depth, int[] count, Map<String, Object> policy) {
        validateComponent(comp, depth, count, policy);
        Object children = comp.get("children");
        if (children instanceof List<?> list && !list.isEmpty()) {
            if (list.size() > bounds.maxChildren()) throw new UniversalException(UniversalFault.INVALID_PAYLOAD, "children exceed " + bounds.maxChildren());
            requireContainer(comp);
            for (Object child : list) if (child instanceof Map<?, ?> m) walkNested((Map<String, Object>) m, depth + 1, count, policy);
        }
    }

    private void requireContainer(Map<String, Object> parent) {
        String type = typeOf(parent);
        if (type != null && registry.types().contains(type) && !registry.containerTypes().contains(type)) {
            throw new UniversalException(UniversalFault.INVALID_PAYLOAD, type + " cannot have children (component " + parent.get("id") + ")");
        }
    }

    private static String typeOf(Map<String, Object> comp) {
        Object t = comp.get("type") != null ? comp.get("type") : comp.get("component");
        return t == null ? null : t.toString();
    }

    private void validateComponent(Map<String, Object> comp, int depth, int[] count, Map<String, Object> policy) {
        if (depth > bounds.maxDepth()) throw new UniversalException(UniversalFault.INVALID_PAYLOAD, "tree depth exceeds " + bounds.maxDepth());
        if (++count[0] > bounds.maxNodes()) throw new UniversalException(UniversalFault.INVALID_PAYLOAD, "node count exceeds " + bounds.maxNodes());
        String type = typeOf(comp);
        if (type != null) {
            requireIdentifier(type, true, "component type");
            if (!registry.types().contains(type)) {
                throw new UniversalException(UniversalFault.INVALID_PAYLOAD, "Invalid component type: " + type + " — allowed: " + new TreeSet<>(registry.types()));
            }
        }
        Object id = comp.get("id");
        if (id != null) requireIdentifier(id.toString(), false, "component id");

        Map<String, Object> props = asMap(comp.get("props"));
        if (props.size() > bounds.maxProps()) throw new UniversalException(UniversalFault.INVALID_PAYLOAD, "props exceed " + bounds.maxProps());
        checkStringLengths(props, "props");
        Map<String, Object> style = asMap(comp.get("style"));
        if (style.size() > bounds.maxProps()) throw new UniversalException(UniversalFault.INVALID_PAYLOAD, "style props exceed " + bounds.maxProps());
        checkStringLengths(style, "style");

        if ("Grid".equals(type)) {
            Object raw = props.get("columns") != null ? props.get("columns") : props.get("numColumns");
            if (raw != null) {
                try {
                    int n = Integer.parseInt(raw.toString());
                    if (n < 1 || n > 12) throw new UniversalException(UniversalFault.INVALID_PAYLOAD, "Grid.columns must be 1-12, got " + raw);
                } catch (NumberFormatException ex) {
                    throw new UniversalException(UniversalFault.INVALID_PAYLOAD, "Grid.columns must be integer 1-12, got " + raw);
                }
            }
        }
        if ("Flex".equals(type)) {
            Object dir = props.get("direction");
            if (dir != null && !FLEX_DIRECTIONS.contains(dir.toString())) throw new UniversalException(UniversalFault.INVALID_PAYLOAD, "Flex.direction must be row|column, got " + dir);
            Object justify = props.get("justify");
            if (justify != null && !FLEX_JUSTIFY.contains(justify.toString())) throw new UniversalException(UniversalFault.INVALID_PAYLOAD, "Flex.justify must be start|center|end|between, got " + justify);
        }
        checkAction(comp, type, props, policy);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        return o instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    private void checkAction(Map<String, Object> comp, String type, Map<String, Object> props, Map<String, Object> policy) {
        Map<String, Object> action = null;
        if (props.get("action") instanceof Map<?, ?>) action = asMap(props.get("action"));
        else if (comp.get("action") instanceof Map<?, ?>) action = asMap(comp.get("action"));
        Object rawBehavior = action != null ? action.get("behavior") : null;
        if (rawBehavior == null && props.get("action") instanceof String s) rawBehavior = s;
        if (rawBehavior == null) rawBehavior = props.get("behavior");
        if (rawBehavior == null) return;
        if (action == null) action = Map.of("behavior", rawBehavior);
        String behavior = rawBehavior.toString();
        if (type != null) {
            Set<String> allowed = registry.allowedBehaviorsByType().getOrDefault(type, Set.of());
            if (registry.behaviors().contains(behavior) && !allowed.contains(behavior)) {
                throw new UniversalException(UniversalFault.INVALID_PAYLOAD, type + " cannot trigger '" + behavior + "'");
            }
        }
        checkActionChain(action, props, policy, 1);
    }

    private void checkActionChain(Map<String, Object> action, Map<String, Object> props, Map<String, Object> policy, int depth) {
        if (depth > bounds.maxChainDepth()) throw new UniversalException(UniversalFault.INVALID_PAYLOAD, "action chain depth exceeds " + bounds.maxChainDepth());
        String behavior = String.valueOf(action.get("behavior"));
        if (!registry.behaviors().contains(behavior)) throw new UniversalException(UniversalFault.INVALID_PAYLOAD, "unknown action behavior '" + behavior + "'");
        if ("navigate".equals(behavior) && action.get("target") == null && props.get("target") == null) {
            throw new UniversalException(UniversalFault.INVALID_PAYLOAD, "navigate action requires target");
        }
        if ("openModal".equals(behavior) && action.get("modalId") == null && props.get("target") == null) {
            throw new UniversalException(UniversalFault.INVALID_PAYLOAD, "openModal action requires modalId (or target)");
        }
        if ("externalLink".equals(behavior)) {
            Object href = action.get("href") != null ? action.get("href") : (props.get("href") != null ? props.get("href") : props.get("url"));
            checkDomain(href == null ? null : href.toString(), "externalLink", policy);
        }
        if ("api".equals(behavior)) {
            Object url = action.get("url");
            if (url == null) throw new UniversalException(UniversalFault.INVALID_PAYLOAD, "api action requires url");
            checkDomain(url.toString(), "api", policy);
        }
        if ("customEvent".equals(behavior) && policy != null) {
            List<String> allowed = stringList(policy, "allowedEvents");
            String name = action.get("eventName") != null ? action.get("eventName").toString()
                    : (props.get("eventName") != null ? props.get("eventName").toString() : "");
            if (!allowed.isEmpty() && !allowed.contains(name)) {
                throw new UniversalException(UniversalFault.INVALID_PAYLOAD, "customEvent '" + name + "' not in policy allowedEvents");
            }
        }
        for (String next : List.of("onSuccess", "onError")) {
            if (action.get(next) instanceof Map<?, ?>) checkActionChain(asMap(action.get(next)), props, policy, depth + 1);
        }
    }

    private void checkDomain(String href, String kind, Map<String, Object> policy) {
        String host = hostOf(href);
        if (host == null) throw new UniversalException(UniversalFault.INVALID_PAYLOAD, kind + " action has unresolvable href");
        if (host.startsWith("http-skip:") || policy == null) return;
        List<String> allowed = stringList(policy, "allowedDomains");
        if (allowed.isEmpty()) return;
        for (String entry : allowed) {
            String rule = entry.startsWith("*.") ? entry.substring(2) : entry;
            if (host.equalsIgnoreCase(rule) || host.toLowerCase().endsWith("." + rule.toLowerCase())) return;
        }
        throw new UniversalException(UniversalFault.INVALID_PAYLOAD, kind + " host '" + host + "' not in policy allowedDomains");
    }

    /** http(s) host, a non-http marker, or null when unresolvable. */
    private static String hostOf(String href) {
        if (href == null || href.isBlank()) return null;
        try {
            URI uri = URI.create(href.trim());
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
            if (scheme.isEmpty()) return null;
            if (!scheme.equals("http") && !scheme.equals("https")) return "http-skip:non-http-scheme";
            String host = uri.getHost();
            return host == null || host.isBlank() ? null : host.toLowerCase();
        } catch (Exception e) {
            return null;
        }
    }

    private void requireIdentifier(String value, boolean allowDot, String kind) {
        if (value == null || value.isEmpty() || value.length() > bounds.maxIdentifierLength()) {
            throw new UniversalException(UniversalFault.INVALID_PAYLOAD, "invalid " + kind + ": '" + value + "'");
        }
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            boolean alpha = (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z');
            boolean digit = ch >= '0' && ch <= '9';
            if (i == 0) {
                if (!(alpha || digit)) throw new UniversalException(UniversalFault.INVALID_PAYLOAD, "invalid " + kind + ": '" + value + "'");
            } else if (!(alpha || digit || ch == '_' || ch == '-' || (allowDot && ch == '.'))) {
                throw new UniversalException(UniversalFault.INVALID_PAYLOAD, "invalid " + kind + ": '" + value + "'");
            }
        }
    }

    private void checkStringLengths(Object value, String path) {
        if (value instanceof String s) {
            if (s.length() > bounds.maxStringLength()) throw new UniversalException(UniversalFault.INVALID_PAYLOAD, "string length exceeds " + bounds.maxStringLength() + " at " + path);
            return;
        }
        if (value instanceof List<?> list) {
            if (list.size() > bounds.maxArrayItems()) throw new UniversalException(UniversalFault.INVALID_PAYLOAD, "array items exceed " + bounds.maxArrayItems() + " at " + path);
            for (int i = 0; i < list.size(); i++) checkStringLengths(list.get(i), path + "[" + i + "]");
            return;
        }
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> e : map.entrySet()) checkStringLengths(e.getValue(), path + "." + e.getKey());
        }
    }

    static List<String> stringList(Map<String, Object> policy, String key) {
        if (policy == null) return List.of();
        Object v = policy.get(key);
        if (!(v instanceof List<?> list)) return List.of();
        List<String> out = new ArrayList<>();
        for (Object o : list) if (o != null) out.add(o.toString());
        return out;
    }
}
