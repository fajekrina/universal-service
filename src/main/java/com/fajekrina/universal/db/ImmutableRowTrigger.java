package com.fajekrina.universal.db;

import org.h2.api.Trigger;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * H2 trigger: rejects UPDATE/DELETE on immutable tables (artefacts, policy, bounds,
 * release delete). A change is always a new row/version.
 */
public class ImmutableRowTrigger implements Trigger {

    private String table;

    @Override
    public void init(Connection conn, String schemaName, String triggerName, String tableName, boolean before, int type) {
        this.table = tableName;
    }

    @Override
    public void fire(Connection conn, Object[] oldRow, Object[] newRow) throws SQLException {
        String op = newRow == null ? "DELETE" : "UPDATE";
        throw new SQLException("Table " + table + " is immutable: create a new version instead of " + op, "23514");
    }
}
