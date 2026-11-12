package com.fajekrina.universal.db;

import org.h2.api.Trigger;

import java.io.Reader;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * H2 trigger for sdui_release: every column is immutable, except the fill-once
 * columns (payload/content_hash at publish, signature/signing_key_id after signing),
 * which may change only from NULL to a value.
 */
public class ReleaseGuardTrigger implements Trigger {

    private static final Set<String> FILL_ONCE = Set.of("PAYLOAD", "CONTENT_HASH", "SIGNATURE", "SIGNING_KEY_ID");
    private final List<String> columns = new ArrayList<>();

    @Override
    public void init(Connection conn, String schemaName, String triggerName, String tableName, boolean before, int type) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? ORDER BY ORDINAL_POSITION")) {
            ps.setString(1, schemaName);
            ps.setString(2, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) columns.add(rs.getString(1).toUpperCase());
            }
        }
    }

    @Override
    public void fire(Connection conn, Object[] oldRow, Object[] newRow) throws SQLException {
        for (int i = 0; i < oldRow.length; i++) {
            String col = i < columns.size() ? columns.get(i) : "#" + i;
            Object before = value(oldRow[i]);
            Object after = value(newRow[i]);
            if (Objects.equals(before, after)) continue;
            if (FILL_ONCE.contains(col) && before == null) continue;
            throw new SQLException("sdui_release is immutable (column " + col + " cannot change once set)", "23514");
        }
    }

    private static Object value(Object v) throws SQLException {
        if (v instanceof Clob clob) {
            try (Reader r = clob.getCharacterStream()) {
                StringBuilder sb = new StringBuilder();
                char[] buf = new char[4096];
                int n;
                while ((n = r.read(buf)) > 0) sb.append(buf, 0, n);
                return sb.toString();
            } catch (java.io.IOException e) {
                throw new SQLException(e);
            }
        }
        return v;
    }
}
