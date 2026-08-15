package com.bemo.shared.db;

import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hibernate statement inspector that logs each executed SQL statement (whitespace-collapsed)
 * together with the current correlation id from the MDC.
 *
 * <p>Register it in your JPA configuration, e.g.
 * {@code spring.jpa.properties.hibernate.session_factory.statement_inspector=com.bemo.shared.db.QueryLoggingStatementInspector},
 * or declare it as a bean of type {@code StatementInspector} when
 * {@code shared.logging.sql.enabled=true}.</p>
 */
public class QueryLoggingStatementInspector implements StatementInspector {

    private static final Logger LOG = LoggerFactory.getLogger(QueryLoggingStatementInspector.class);

    @Override
    public String inspect(String sql) {
        if (sql != null && !sql.isBlank() && LOG.isDebugEnabled()) {
            LOG.debug("SQL [{}]: {}", correlation(), compact(sql));
        }
        return sql;
    }

    private String correlation() {
        String requestId = org.slf4j.MDC.get("requestId");
        return requestId == null ? "no-correlation-id" : requestId;
    }

    private String compact(String sql) {
        String trimmed = sql.replaceAll("\\s+", " ").trim();
        return trimmed.length() > 400 ? trimmed.substring(0, 400) + "..." : trimmed;
    }
}
