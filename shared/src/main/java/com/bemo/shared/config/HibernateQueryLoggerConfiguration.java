package com.bemo.shared.config;

import com.bemo.shared.db.QueryLoggingStatementInspector;

import org.hibernate.SessionFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

/**
 * Registers a Hibernate {@code StatementInspector} that logs every SQL statement emitted by
 * the application, bound to the current correlation id so slow or heavy queries can be traced.
 *
 * <p>Opt-in via {@code shared.logging.sql.enabled=true} (defaults to off) or on by default
 * for local development when the {@code shared.logging.sql.debug} profile flag is used.</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({SessionFactory.class, LocalContainerEntityManagerFactoryBean.class})
public class HibernateQueryLoggerConfiguration {

    @Bean
    @ConditionalOnProperty(name = "shared.logging.sql.enabled", havingValue = "true")
    public QueryLoggingStatementInspector queryLoggingStatementInspector() {
        return new QueryLoggingStatementInspector();
    }
}
