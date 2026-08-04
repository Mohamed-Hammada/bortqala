package com.bemo.hr;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the production upgrade path: applying the released baseline
 * changelog first (the "current production schema") and then the full master
 * changelog must reach exactly the same schema state as a fresh install.
 * Runs directly against the shared Testcontainers PostgreSQL instance with a
 * throwaway database, so it does not disturb the Spring context.
 */
class LiquibaseUpgradePathTests {

    private static final String BASELINE =
            "db/changelog/releases/20260729_v1_v67.changelog-master.yaml";
    private static final String FULL_MASTER = "db/changelog/db.changelog-master.yaml";

    @Test
    void upgradeFromReleasedBaselineReachesTheSameSchemaAsFreshInstall() throws Exception {
        PostgreSQLContainer<?> postgres = PostgresIntegrationTest.POSTGRES;
        String baseUrl = postgres.getJdbcUrl();
        String hostPart = baseUrl.substring(0, baseUrl.lastIndexOf('/'));
        String maintenanceUrl = hostPart + "/postgres";
        String upgradeDb = "bemo_upgrade_test";
        String freshDb = "bemo_upgrade_fresh_test";

        try (Connection admin = DriverManager.getConnection(
                maintenanceUrl, postgres.getUsername(), postgres.getPassword())) {
            dropDatabase(admin, upgradeDb);
            dropDatabase(admin, freshDb);
            createDatabase(admin, upgradeDb);
            createDatabase(admin, freshDb);
        }

        try {
            int baselineCount = apply(hostPart + "/" + upgradeDb, postgres, BASELINE);
            int upgradedTotal = apply(hostPart + "/" + upgradeDb, postgres, FULL_MASTER);
            int freshTotal = apply(hostPart + "/" + freshDb, postgres, FULL_MASTER);

            assertThat(upgradedTotal)
                    .as("the upgrade must apply new changesets beyond the released baseline")
                    .isGreaterThan(baselineCount);
            assertThat(upgradedTotal)
                    .as("an upgraded database must end with the same changeset count as a fresh install")
                    .isEqualTo(freshTotal);
        } finally {
            try (Connection admin = DriverManager.getConnection(
                    maintenanceUrl, postgres.getUsername(), postgres.getPassword())) {
                dropDatabase(admin, upgradeDb);
                dropDatabase(admin, freshDb);
            }
        }
    }

    private int apply(String url, PostgreSQLContainer<?> postgres, String changelog) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                url, postgres.getUsername(), postgres.getPassword())) {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            Liquibase liquibase = new Liquibase(changelog, new ClassLoaderResourceAccessor(), database);
            liquibase.update();
            try (Statement statement = connection.createStatement();
                 ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM databasechangelog")) {
                rows.next();
                return rows.getInt(1);
            }
        }
    }

    private void createDatabase(Connection connection, String name) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + name);
        }
    }

    private void dropDatabase(Connection connection, String name) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS " + name + " WITH (FORCE)");
        }
    }
}
