package com.fajekrina.universal.utils;

import com.fajekrina.universal.utils.enums.UniversalFault;
import com.fajekrina.universal.utils.exception.UniversalException;

import java.util.*;

/**
 * S1 splitter: monolithic builder payload → tree artefact + script artefact.
 *
 * Deterministic and lossless:
 * <ul>
 *   <li>flat {@code parentId + order} (or nested {@code children}) becomes one nested tree;</li>
 *   <li>inline actions move to the script as {@code a_<12 hex sha256(canonical action)>} — identical actions share a name;</li>
 *   <li>style / token / sharedStyle and unknown keys are kept as-is (tokenisation is an explicit editor action).</li>
 * </ul>
 * Pre-order numbering of the result reproduces the original {@code order} (see {@link PayloadAssembler}).
 */
public final class ArtifactSplitter {

    private static final Set<String> RESERVED = Set.of(
            "id", "type", "component", "props", "style", "token", "sharedStyle", "actionRef",
            "children", "parentId", "order", "action");

    public record Split(Map<String, Object> tree, Map<String, Object> script, int nodeCount, int maxDepth) {}

    private ArtifactSplitter() {}

    @SuppressWarnings("unchecked")
    public static Split split(Map<String, Object> layout, String schemaVersion) {
        List<Map<String, Object>> components = new ArrayList<>();
        Object raw = layout == null ? null : layout.get("components");
        if (raw instanceof List<?> list) {
            for (Object o : list) if (o instanceof Map<?, ?> m) components.add((Map<String, Object>) m);
        }

        Map<String, Object> actions = new TreeMap<>();
        Set<String> events = new TreeSet<>();
        int[] stats = new int[]{0, 0}; // nodes, max depth

        List<Map<String, Object>> roots = new ArrayList<>();
        boolean flat = components.stream().anyMatch(c -> c.get("parentId") != null);
        if (flat) {
            Map<String, Map<String, Object>> byId = new LinkedHashMap<>();
            for (Map<String, Object> c : components) byId.put(String.valueOf(c.get("id")), c);
            Map<String, List<Map<String, Object>>> kids = new HashMap<>();
            for (Map<String, Object> c : components) {
                Object pid = c.get("parentId");
                String key = pid != null && byId.containsKey(pid.toString()) ? pid.toString() : null;
                kids.computeIfAbsent(key, k -> new ArrayList<>()).add(c);
            }
            for (Map<String, Object> c : sorted(kids.getOrDefault(null, List.of()))) {
                roots.add(toNode(c, kids, actions, events, 1, stats, new HashSet<>()));
            }
        } else {
            for (Map<String, Object> c : sorted(components)) {
                roots.add(toNode(c, null, actions, events, 1, stats, new HashSet<>()));
            }
        }

        Map<String, Object> layoutMeta = new LinkedHashMap<>();
        layoutMeta.put("type", layout != null && layout.get("type") != null ? layout.get("type") : "Container");
        if (layout != null && layout.get("title") != null) layoutMeta.put("title", layout.get("title"));
        if (layout != null && layout.get("subtitle") != null) layoutMeta.put("subtitle", layout.get("subtitle"));

        Map<String, Object> tree = new LinkedHashMap<>();
        tree.put("schemaVersion", schemaVersion);
        tree.put("layout", layoutMeta);
        tree.put("nodes", roots);

        Map<String, Object> script = new LinkedHashMap<>();
        script.put("actions", actions);
        script.put("events", new ArrayList<>(events));
        return new Split(tree, script, stats[0], stats[1]);
    }

    private static List<Map<String, Object>> sorted(List<Map<String, Object>> list) {
        List<Map<String, Object>> copy = new ArrayList<>(list);
        // stable: equal/missing order keeps input order
        copy.sort(Comparator.comparingDouble(c -> {
            Object o = c.get("order");
            if (o instanceof Number n) return n.doubleValue();
            try { return o == null ? Double.MAX_VALUE : Double.parseDouble(o.toString()); } catch (NumberFormatException e) { return Double.MAX_VALUE; }
        }));
        return copy;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> toNode(Map<String, Object> c,
                                              Map<String, List<Map<String, Object>>> flatKids,
                                              Map<String, Object> actions, Set<String> events,
                                              int depth, int[] stats, Set<String> path) {
        String id = String.valueOf(c.get("id"));
        if (!path.add(id)) throw new UniversalException(UniversalFault.INVALID_PAYLOAD, "parent cycle at component " + id);
        stats[0]++;
        stats[1] = Math.max(stats[1], depth);

        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id", id);
        Object type = c.get("type") != null ? c.get("type") : c.get("component");
        node.put("type", type);

        Map<String, Object> props = c.get("props") instanceof Map<?, ?> p ? new LinkedHashMap<>((Map<String, Object>) p) : null;
        Map<String, Object> action = null;
        if (props != null && props.get("action") instanceof Map<?, ?> a) action = new LinkedHashMap<>((Map<String, Object>) a);
        else if (c.get("action") instanceof Map<?, ?> a) action = new LinkedHashMap<>((Map<String, Object>) a);
        else if (props != null && props.get("action") instanceof String s && !s.isBlank()) action = new LinkedHashMap<>(Map.of("behavior", s));
        if (props != null) props.remove("action");
        if (props != null) node.put("props", props);
        if (c.get("style") instanceof Map<?, ?> s) node.put("style", new LinkedHashMap<>((Map<String, Object>) s));
        if (c.get("token") instanceof Map<?, ?> t) node.put("token", new LinkedHashMap<>((Map<String, Object>) t));
        if (c.get("sharedStyle") instanceof String ss && !ss.isBlank()) node.put("sharedStyle", ss);

        if (action != null && action.get("behavior") != null) {
            String name = "a_" + CanonicalJson.sha256Hex(CanonicalJson.canonical(action)).substring(0, 12);
            actions.put(name, action);
            if ("customEvent".equals(String.valueOf(action.get("behavior"))) && action.get("eventName") != null) {
                events.add(action.get("eventName").toString());
            }
            node.put("actionRef", name);
        } else if (c.get("actionRef") instanceof String ref && !ref.isBlank()) {
            node.put("actionRef", ref);
        }

        for (Map.Entry<String, Object> e : c.entrySet()) {
            if (!RESERVED.contains(e.getKey())) node.put(e.getKey(), e.getValue());
        }

        List<Map<String, Object>> children = new ArrayList<>();
        if (flatKids != null) {
            for (Map<String, Object> ch : sorted(flatKids.getOrDefault(id, List.of()))) {
                children.add(toNode(ch, flatKids, actions, events, depth + 1, stats, path));
            }
        } else if (c.get("children") instanceof List<?> list) {
            List<Map<String, Object>> nested = new ArrayList<>();
            for (Object o : list) if (o instanceof Map<?, ?> m) nested.add((Map<String, Object>) m);
            for (Map<String, Object> ch : sorted(nested)) {
                children.add(toNode(ch, null, actions, events, depth + 1, stats, path));
            }
        }
        if (!children.isEmpty()) node.put("children", children);
        path.remove(id);
        return node;
    }
}
