package com.bemo.hr;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for integration tests that must exercise real PostgreSQL row
 * locking, transaction waiting, and isolation. Runs the exact production
 * Liquibase changelog and validates the entity mapping like production does.
 * <p>
 * The container is started exactly once per JVM and kept alive until the
 * process exits. It is intentionally NOT a JUnit {@code @Container}: a
 * per-class lifecycle would restart Postgres between test classes while the
 * Spring TestContext cache keeps the first class's datasource URL, so later
 * classes would connect to a port where no Postgres is listening.
 */
@SpringBootTest
public abstract class PostgresIntegrationTest {

    public static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
                .withDatabaseName("bemo_test")
                .withUsername("test")
                .withPassword("test");
        POSTGRES.start();
        Runtime.getRuntime().addShutdownHook(new Thread(POSTGRES::stop));
    }

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.liquibase.change-log",
                () -> "classpath:db/changelog/test-postgresql.changelog-master.yaml");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }
}
