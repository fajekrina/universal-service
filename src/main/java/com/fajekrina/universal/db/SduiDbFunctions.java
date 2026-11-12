package com.fajekrina.universal.db;

import com.fajekrina.universal.utils.CanonicalJson;
import com.fajekrina.universal.utils.PayloadAssembler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Java functions behind {@code v_sdui_release_payload} (H2 {@code CREATE ALIAS}).
 * H2 has no JSON manipulation, so the view delegates to {@link PayloadAssembler};
 * the view is still the single place that defines "assembled payload of a release".
 */
public final class SduiDbFunctions {

    private SduiDbFunctions() {}

    private record Loaded(Map<String, Object> envelope, PayloadAssembler.Result result) {}

    private static final String SQL = """
            SELECT r.release_id, r.page_id, r.merchant_id, r.sequence, r.published_at, r.policy_version,
                   p.page_key, t.body AS tree_body, s.body AS style_body, sc.body AS script_body,
                   pol.allowed_domains, pol.allowed_events
            FROM sdui_release r
            JOIN sdui_page p ON p.page_id = r.page_id
            JOIN sdui_tree t ON t.tree_id = r.tree_id
            LEFT JOIN sdui_style s ON s.style_id = r.style_id
            LEFT JOIN sdui_script sc ON sc.script_id = r.script_id
            JOIN sdui_policy pol ON pol.merchant_id = r.merchant_id AND pol.policy_version = r.policy_version
            WHERE r.release_id = ?""";

    private static Loaded load(Connection conn, Long releaseId) throws SQLException {
        if (releaseId == null) return null;
        try (PreparedStatement ps = conn.prepareStatement(SQL)) {
            ps.setLong(1, releaseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Map<String, Object> tree = CanonicalJson.readMap(rs.getString("tree_body"));
                Map<String, Object> style = CanonicalJson.readMap(rs.getString("style_body"));
                Map<String, Object> script = CanonicalJson.readMap(rs.getString("script_body"));
                PayloadAssembler.Result result = PayloadAssembler.flatten(tree, style, script);

                String merchantId = rs.getString("merchant_id");
                int sequence = rs.getInt("sequence");
                OffsetDateTime publishedAt = rs.getObject("published_at", OffsetDateTime.class);

                Map<String, Object> env = new LinkedHashMap<>();
                env.put("pageId", rs.getString("page_id"));
                env.put("pageKey", rs.getString("page_key"));
                env.put("merchantId", merchantId);
                env.put("status", "PUBLISHED");
                env.put("schemaVersion", tree == null ? "1.0" : String.valueOf(tree.getOrDefault("schemaVersion", "1.0")));
                env.put("version", sequence);
                env.put("releaseSequence", sequence);
                env.put("publishedAt", publishedAt == null ? null : publishedAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                env.put("policy", PayloadAssembler.policyBlock(merchantId, rs.getInt("policy_version"),
                        CanonicalJson.readStringList(rs.getString("allowed_domains")),
                        CanonicalJson.readStringList(rs.getString("allowed_events"))));
                env.put("layout", PayloadAssembler.layout(tree, result.components()));
                return new Loaded(env, result);
            }
        }
    }

    /** Assembled A1 payload (compact JSON) of a release. */
    public static String assemble(Connection conn, Long releaseId) throws SQLException {
        Loaded l = load(conn, releaseId);
        return l == null ? null : CanonicalJson.write(l.envelope());
    }

    /** JSON array of refs that did not resolve ([{node, token|sharedStyle|actionRef}]). */
    public static String unresolved(Connection conn, Long releaseId) throws SQLException {
        Loaded l = load(conn, releaseId);
        return l == null ? "[]" : CanonicalJson.write(l.result().unresolved());
    }

    public static Integer nodeCount(Connection conn, Long releaseId) throws SQLException {
        Loaded l = load(conn, releaseId);
        return l == null ? 0 : l.result().nodeCount();
    }
}
