package com.bemo.hr.attendance;

import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the V92 migration backfills a source identity for every historical
 * punch before the {@code NOT NULL} constraint is applied. Rebuilds the
 * pre-V92 schema in an in-memory H2 database, seeds live-sync and file-import
 * rows, then applies only the V92 changesets through Liquibase.
 */
class PunchSourceIdentityMigrationTests {

    @Test
    void backfillPopulatesSourceKeysBeforeNotNullConstraint() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:migration-v92;DB_CLOSE_DELAY=-1", "sa", "")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                        CREATE TABLE import_batches (
                            id VARCHAR(36) PRIMARY KEY,
                            app_id VARCHAR(36) NOT NULL,
                            checksum VARCHAR(64) NOT NULL,
                            file_name VARCHAR(255) NOT NULL,
                            device_name VARCHAR(150) NOT NULL,
                            status VARCHAR(20) NOT NULL,
                            total_rows INT NOT NULL,
                            imported_rows INT NOT NULL,
                            error_rows INT NOT NULL,
                            imported_by VARCHAR(100) NOT NULL,
                            imported_at TIMESTAMP NOT NULL
                        )
                        """);
                statement.execute("""
                        CREATE TABLE punch_records (
                            id VARCHAR(36) PRIMARY KEY,
                            app_id VARCHAR(36) NOT NULL,
                            batch_id VARCHAR(36) NOT NULL,
                            device_id VARCHAR(36),
                            employee_id VARCHAR(36),
                            device_user_id VARCHAR(100) NOT NULL,
                            raw_name VARCHAR(200),
                            punched_at TIMESTAMP WITH TIME ZONE NOT NULL,
                            raw_line TEXT NOT NULL,
                            row_number INT NOT NULL
                        )
                        """);
                statement.execute("""
                        INSERT INTO import_batches (id, app_id, checksum, file_name, device_name, status, total_rows, imported_rows, error_rows, imported_by, imported_at)
                        VALUES ('b1', 'app1', 'checksum-1', 'attendance.csv', 'Gate One', 'COMPLETED', 2, 2, 0, 'tester', CURRENT_TIMESTAMP)
                        """);
                statement.execute("""
                        INSERT INTO punch_records (id, app_id, batch_id, device_id, employee_id, device_user_id, raw_name, punched_at, raw_line, row_number)
                        VALUES
                          ('p-live', 'app1', 'b1', 'dev-1', NULL, 'U-1', 'Live User', TIMESTAMP WITH TIME ZONE '2026-08-04 08:00:00', 'line-1', 1),
                          ('p-file', 'app1', 'b1', NULL, NULL, 'U-1', 'File User', TIMESTAMP WITH TIME ZONE '2026-08-04 09:00:00', 'line-2', 2)
                        """);
            }

            var database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            Liquibase liquibase = new Liquibase(
                    "db/changelog/schema/update/20260804_v92_punch_source_identity.yaml",
                    new ClassLoaderResourceAccessor(), database);
            liquibase.update("");

            try (Statement statement = connection.createStatement()) {
                try (ResultSet nullable = statement.executeQuery(
                        "SELECT is_nullable FROM information_schema.columns "
                                + "WHERE table_name = 'PUNCH_RECORDS' AND column_name = 'SOURCE_KEY'")) {
                    assertThat(nullable.next()).isTrue();
                    assertThat(nullable.getString(1)).as("source_key becomes NOT NULL").isEqualTo("NO");
                }

                try (ResultSet live = statement.executeQuery(
                        "SELECT source_key FROM punch_records WHERE id = 'p-live'")) {
                    assertThat(live.next()).isTrue();
                    assertThat(live.getString(1)).isEqualTo("DEVICE:dev-1");
                }
                try (ResultSet file = statement.executeQuery(
                        "SELECT source_key FROM punch_records WHERE id = 'p-file'")) {
                    assertThat(file.next()).isTrue();
                    assertThat(file.getString(1)).isEqualTo("FILE_DEVICE:Gate One");
                }
            }
        }
    }
}
