package com.bemo.shared.db;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class QueryLoggingStatementInspectorTest {

    @Test
    void inspectReturnsSqlUnchanged() {
        QueryLoggingStatementInspector inspector = new QueryLoggingStatementInspector();
        String sql = "select * from employees where tenant_id = ?";
        assertEquals(sql, inspector.inspect(sql));
    }

    @Test
    void inspectHandlesNullAndBlank() {
        QueryLoggingStatementInspector inspector = new QueryLoggingStatementInspector();
        assertEquals(null, inspector.inspect(null));
        assertEquals("", inspector.inspect(""));
    }
}
