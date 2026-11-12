package com.fajekrina.universal.utils;

import java.util.*;

/**
 * Assembles the 3 artefacts back into the A1 payload (flat {@code parentId + order}).
 * Used by the view function {@code SDUI_ASSEMBLE} (releases) and by the draft read
 * (builder), so there is exactly one assembly implementation.
 *
 * Per node: style = shared (style artefact) &lt; token (style artefact) &lt; inline;
 * {@code actionRef} → {@code props.action}. Library presets are NOT applied (client core does that).
 */
public final class PayloadAssembler {

    private PayloadAssembler() {}

    public record Result(List<Map<String, Object>> components, List<Map<String, Object>> unresolved, int nodeCount) {}

    @SuppressWarnings("unchecked")
    public static Result flatten(Map<String, Object> tree, Map<String, Object> style, Map<String, Object> script) {
        List<Map<String, Object>> out = new ArrayList<>();
        List<Map<String, Object>> unresolved = new ArrayList<>();
        Map<String, Object> shared = style != null && style.get("shared") instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
        Map<String, String> tokens = flattenTokens(style);
        Map<String, Object> actions = script != null && script.get("actions") instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
        int[] order = new int[]{0};
        Object nodes = tree == null ? null : tree.get("nodes");
        if (nodes instanceof List<?> list) {
            for (Object n : list) {
                if (n instanceof Map<?, ?> node) walk((Map<String, Object>) node, null, out, unresolved, shared, tokens, actions, order);
            }
        }
        return new Result(out, unresolved, out.size());
    }

    @SuppressWarnings("unchecked")
    private static void walk(Map<String, Object> node, String parentId, List<Map<String, Object>> out,
                             List<Map<String, Object>> unresolved, Map<String, Object> shared,
                             Map<String, String> tokens, Map<String, Object> actions, int[] order) {
        String id = String.valueOf(node.get("id"));
        Map<String, Object> comp = new LinkedHashMap<>();
        comp.put("id", id);
        comp.put("component", node.get("type"));
        comp.put("parentId", parentId);

        Map<String, Object> props = node.get("props") instanceof Map<?, ?> p ? new LinkedHashMap<>((Map<String, Object>) p) : null;
        Object ref = node.get("actionRef");
        if (ref != null) {
            Object action = actions.get(ref.toString());
            if (action instanceof Map<?, ?>) {
                if (props == null) props = new LinkedHashMap<>();
                props.put("action", action);
            } else {
                unresolved.add(Map.of("node", id, "actionRef", ref.toString()));
            }
        }
        if (props != null) comp.put("props", props);

        Map<String, Object> merged = new LinkedHashMap<>();
        boolean hasStyle = node.containsKey("style");
        Object sharedName = node.get("sharedStyle");
        if (sharedName != null) {
            Object css = shared.get(sharedName.toString());
            if (css instanceof Map<?, ?> m) merged.putAll((Map<String, Object>) m);
            else unresolved.add(Map.of("node", id, "sharedStyle", sharedName.toString()));
            hasStyle = true;
        }
        if (node.get("token") instanceof Map<?, ?> tok) {
            for (Map.Entry<?, ?> e : tok.entrySet()) {
                String name = String.valueOf(e.getValue());
                String value = tokens.get(name);
                if (value != null) merged.put(String.valueOf(e.getKey()), value);
                else unresolved.add(Map.of("node", id, "token", name));
            }
            hasStyle = true;
        }
        if (node.get("style") instanceof Map<?, ?> inline) merged.putAll((Map<String, Object>) inline);
        if (hasStyle) comp.put("style", merged);
        comp.put("order", order[0]++);

        for (Map.Entry<String, Object> e : node.entrySet()) {
            String k = e.getKey();
            if (!Set.of("id", "type", "props", "style", "token", "sharedStyle", "actionRef", "children").contains(k)) comp.put(k, e.getValue());
        }
        out.add(comp);

        if (node.get("children") instanceof List<?> kids) {
            for (Object ch : kids) {
                if (ch instanceof Map<?, ?> m) walk((Map<String, Object>) m, id, out, unresolved, shared, tokens, actions, order);
            }
        }
    }

    @SuppressWarnings("unchecked")
    static Map<String, String> flattenTokens(Map<String, Object> style) {
        Map<String, String> out = new HashMap<>();
        if (style == null || !(style.get("tokens") instanceof Map<?, ?> cats)) return out;
        for (Object cat : ((Map<String, Object>) cats).values()) {
            if (cat instanceof Map<?, ?> m) {
                for (Map.Entry<?, ?> e : m.entrySet()) out.put(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
            }
        }
        return out;
    }

    /** layout block: {type, title?, subtitle?, components} */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> layout(Map<String, Object> tree, List<Map<String, Object>> components) {
        Map<String, Object> meta = tree != null && tree.get("layout") instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
        Map<String, Object> layout = new LinkedHashMap<>();
        layout.put("type", meta.getOrDefault("type", "Container"));
        if (meta.get("title") != null) layout.put("title", meta.get("title"));
        if (meta.get("subtitle") != null) layout.put("subtitle", meta.get("subtitle"));
        layout.put("components", components);
        return layout;
    }

    /** Policy block as shipped to clients. */
    public static Map<String, Object> policyBlock(String merchantId, int policyVersion, List<String> domains, List<String> events) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("merchantId", merchantId);
        p.put("policyVersion", String.valueOf(policyVersion));
        p.put("allowedDomains", domains);
        p.put("allowedEvents", events);
        return p;
    }
}
