package com.bemo.hr.attendance;

import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the V92 migration against real PostgreSQL: it registers a stable
 * biometric source for every live device and every distinct file source,
 * maps every historical punch and batch, records batch evidence, collapses
 * duplicate punches onto one row, and only then applies the NOT NULL and
 * source-scoped uniqueness constraints. Runs against a throwaway database on
 * the shared Testcontainers PostgreSQL instance.
 */
class PunchSourceIdentityMigrationTests {

    private static final String PRE_V92 =
            "db/changelog/test-v92-pre.changelog-master.yaml";
    private static final String V92 =
            "db/changelog/schema/update/20260804_v92_punch_source_identity.yaml";

    @Test
    void backfillRegistersSourcesLinksPunchesAndDedupesBeforeNotNull() throws Exception {
        PostgreSQLContainer<?> postgres = com.bemo.hr.PostgresIntegrationTest.POSTGRES;
        String baseUrl = postgres.getJdbcUrl();
        String hostPart = baseUrl.substring(0, baseUrl.lastIndexOf('/'));
        String migrationDb = "bemo_v92_migration_test";
        String deviceId = UUID.randomUUID().toString();

        try (Connection admin = DriverManager.getConnection(
                hostPart + "/postgres", postgres.getUsername(), postgres.getPassword())) {
            try (Statement statement = admin.createStatement()) {
                statement.execute("DROP DATABASE IF EXISTS " + migrationDb + " WITH (FORCE)");
                statement.execute("CREATE DATABASE " + migrationDb);
            }
        }

        try (Connection connection = DriverManager.getConnection(
                hostPart + "/" + migrationDb, postgres.getUsername(), postgres.getPassword())) {
            apply(connection, PRE_V92);
            seedLegacyData(connection, deviceId);
            apply(connection, V92);

            try (Statement statement = connection.createStatement()) {
                assertSourceCatalog(statement);
                assertPunchMappings(statement, deviceId);
                assertEvidenceAndDeduplication(statement);
                assertConstraints(statement);
            }
        } finally {
            try (Connection admin = DriverManager.getConnection(
                    hostPart + "/postgres", postgres.getUsername(), postgres.getPassword())) {
                try (Statement statement = admin.createStatement()) {
                    statement.execute("DROP DATABASE IF EXISTS " + migrationDb + " WITH (FORCE)");
                }
            }
        }
    }

    private void apply(Connection connection, String changelog) throws Exception {
        Liquibase liquibase = new Liquibase(changelog, new ClassLoaderResourceAccessor(),
                DatabaseFactory.getInstance()
                        .findCorrectDatabaseImplementation(new JdbcConnection(connection)));
        liquibase.update("");
    }

    private void seedLegacyData(Connection connection, String deviceId) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    INSERT INTO apps (id, code, name) VALUES ('app1', 'APP-1', 'Migration App')
                    """);
            statement.execute("""
                    INSERT INTO biometric_devices
                        (id, app_id, name, endpoint_url, enabled, sync_interval_minutes,
                         last_status, created_at, updated_at)
                    VALUES
                        ('%s', 'app1', 'Main Gate', 'http://192.168.1.50/api/punches',
                         TRUE, 15, 'SUCCESS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """.formatted(deviceId));
            statement.execute("""
                    INSERT INTO import_batches
                        (id, app_id, checksum, file_name, device_name, status, total_rows, imported_rows, error_rows, imported_by, imported_at)
                    VALUES
                        ('b-live', 'app1', 'checksum-live', 'device-sync-%s-1730000000000.json', 'Main Gate', 'COMPLETED', 1, 1, 0, 'tester', CURRENT_TIMESTAMP),
                        ('b-file', 'app1', 'checksum-file', 'attendance.csv', '  Gate One  ', 'COMPLETED', 3, 2, 0, 'tester', CURRENT_TIMESTAMP),
                        ('b-file2', 'app1', 'checksum-file2', 'attendance-copy.csv', '  Gate One  ', 'COMPLETED', 2, 2, 0, 'tester', CURRENT_TIMESTAMP)
                    """.formatted(deviceId));
            statement.execute("""
                    INSERT INTO punch_records
                        (id, app_id, batch_id, device_id, employee_id, device_user_id, raw_name, punched_at, raw_line, row_number)
                    VALUES
                        ('p-live', 'app1', 'b-live', '%s', NULL, 'U-1', 'Live User',
                         TIMESTAMP WITH TIME ZONE '2026-08-04 08:00:00', 'live-line', 1),
                        ('p-file', 'app1', 'b-file', NULL, NULL, 'U-1', 'File User',
                         TIMESTAMP WITH TIME ZONE '2026-08-04 09:00:00', 'file-line', 1),
                        ('p-file2', 'app1', 'b-file2', NULL, NULL, 'U-1', 'File User Copy',
                         TIMESTAMP WITH TIME ZONE '2026-08-04 09:00:00', 'file-line-copy', 1),
                        ('p-file3', 'app1', 'b-file', NULL, NULL, 'U-2', 'Second User',
                         TIMESTAMP WITH TIME ZONE '2026-08-04 10:00:00', 'second-line', 2)
                    """.formatted(deviceId));
        }
    }

    private void assertSourceCatalog(Statement statement) throws Exception {
        try (ResultSet rows = statement.executeQuery("""
                SELECT source_type, normalized_code, active
                FROM biometric_sources WHERE app_id = 'app1'
                ORDER BY source_type, normalized_code
                """)) {
            assertThat(rows.next()).isTrue();
            assertThat(rows.getString(1)).isEqualTo("DEVICE");
            assertThat(rows.next()).isTrue();
            assertThat(rows.getString(1)).isEqualTo("FILE_DEVICE");
            assertThat(rows.getString(2)).isEqualTo("gate one");
        }
        try (ResultSet deviceSources = statement.executeQuery("""
                SELECT COUNT(*) FROM biometric_sources
                WHERE app_id = 'app1' AND source_type = 'DEVICE'
                """)) {
            deviceSources.next();
            assertThat(deviceSources.getInt(1)).isEqualTo(1);
        }
        try (ResultSet fileSources = statement.executeQuery("""
                SELECT COUNT(*) FROM biometric_sources
                WHERE app_id = 'app1' AND source_type = 'FILE_DEVICE' AND normalized_code = 'gate one'
                """)) {
            fileSources.next();
            assertThat(fileSources.getInt(1)).isEqualTo(1);
        }
    }

    private void assertPunchMappings(Statement statement, String deviceId) throws Exception {
        try (ResultSet rows = statement.executeQuery("""
                SELECT p.id, s.source_type, s.normalized_code
                FROM punch_records p
                JOIN biometric_sources s ON s.id = p.source_id
                WHERE p.app_id = 'app1'
                ORDER BY s.source_type, p.id
                """)) {
            assertThat(rows.next()).isTrue();
            assertThat(rows.getString(1)).isEqualTo("p-live");
            assertThat(rows.getString(2)).isEqualTo("DEVICE");
            assertThat(rows.getString(3)).isEqualTo(deviceId);

            assertThat(rows.next()).isTrue();
            assertThat(rows.getString(1)).isEqualTo("p-file");
            assertThat(rows.getString(2)).isEqualTo("FILE_DEVICE");
            assertThat(rows.getString(3)).isEqualTo("gate one");

            assertThat(rows.next()).isTrue();
            assertThat(rows.getString(1)).isEqualTo("p-file3");
            assertThat(rows.getString(2)).isEqualTo("FILE_DEVICE");
            assertThat(rows.getString(3)).isEqualTo("gate one");

            assertThat(rows.next()).isFalse();
        }
    }

    private void assertEvidenceAndDeduplication(Statement statement) throws Exception {
        try (ResultSet duplicate = statement.executeQuery(
                "SELECT COUNT(*) FROM punch_records WHERE id = 'p-file2'")) {
            duplicate.next();
            assertThat(duplicate.getInt(1)).as("duplicate punch is collapsed onto the lexicographically smallest id")
                    .isZero();
        }
        try (ResultSet evidence = statement.executeQuery("""
                SELECT punch_id, batch_id, row_number FROM punch_import_evidence
                ORDER BY punch_id, batch_id
                """)) {
            assertThat(evidence.next()).isTrue();
            assertThat(evidence.getString(1)).isEqualTo("p-file");
            assertThat(evidence.getString(2)).isEqualTo("b-file");
            assertThat(evidence.getInt(3)).isEqualTo(1);
            assertThat(evidence.next()).isTrue();
            assertThat(evidence.getString(1)).isEqualTo("p-file");
            assertThat(evidence.getString(2)).isEqualTo("b-file2");
            assertThat(evidence.getInt(3)).isEqualTo(1);
            assertThat(evidence.next()).isTrue();
            assertThat(evidence.getString(1)).isEqualTo("p-file3");
            assertThat(evidence.getString(2)).isEqualTo("b-file");
            assertThat(evidence.getInt(3)).isEqualTo(2);
            assertThat(evidence.next()).isTrue();
            assertThat(evidence.getString(1)).isEqualTo("p-live");
            assertThat(evidence.getString(2)).isEqualTo("b-live");
            assertThat(evidence.getInt(3)).isEqualTo(1);
            assertThat(evidence.next()).isFalse();
        }
        try (ResultSet counts = statement.executeQuery("""
                SELECT id, valid_rows, imported_rows, new_punches, duplicate_punches
                FROM import_batches
                WHERE app_id = 'app1' ORDER BY id
                """)) {
            assertThat(counts.next()).isTrue();
            assertThat(counts.getString(1)).isEqualTo("b-file");
            assertThat(counts.getInt(2)).as("valid rows never count punch_records").isEqualTo(3);
            assertThat(counts.getInt(3)).isEqualTo(3);
            assertThat(counts.getInt(4)).isEqualTo(2);
            assertThat(counts.getInt(5)).isEqualTo(1);
            assertThat(counts.next()).isTrue();
            assertThat(counts.getString(1)).isEqualTo("b-file2");
            assertThat(counts.getInt(2)).as("duplicate file still reports its valid rows").isEqualTo(2);
            assertThat(counts.getInt(3)).isEqualTo(2);
            assertThat(counts.getInt(4)).isEqualTo(1);
            assertThat(counts.getInt(5)).isEqualTo(1);
            assertThat(counts.next()).isTrue();
            assertThat(counts.getString(1)).isEqualTo("b-live");
            assertThat(counts.getInt(2)).isEqualTo(1);
            assertThat(counts.getInt(3)).isEqualTo(1);
            assertThat(counts.getInt(4)).isEqualTo(1);
            assertThat(counts.getInt(5)).isZero();
            assertThat(counts.next()).isFalse();
        }
    }

    private void assertConstraints(Statement statement) throws Exception {
        try (ResultSet sourceNotNull = statement.executeQuery("""
                SELECT is_nullable FROM information_schema.columns
                WHERE table_name = 'punch_records' AND column_name = 'source_id'
                """)) {
            assertThat(sourceNotNull.next()).isTrue();
            assertThat(sourceNotNull.getString(1)).as("source_id becomes NOT NULL").isEqualTo("NO");
        }
        try (ResultSet batchNotNull = statement.executeQuery("""
                SELECT is_nullable FROM information_schema.columns
                WHERE table_name = 'import_batches' AND column_name = 'source_id'
                """)) {
            assertThat(batchNotNull.next()).isTrue();
            assertThat(batchNotNull.getString(1)).as("import_batches.source_id becomes NOT NULL").isEqualTo("NO");
        }
        try (ResultSet newIndex = statement.executeQuery("""
                SELECT indexname FROM pg_indexes
                WHERE tablename = 'punch_records' AND indexname = 'uq_punch_records_source_user_time'
                """)) {
            assertThat(newIndex.next()).as("source-scoped dedup index exists").isTrue();
        }
        try (ResultSet legacyIndex = statement.executeQuery("""
                SELECT indexname FROM pg_indexes
                WHERE tablename = 'punch_records' AND indexname = 'uq_punch_records_app_device_user_time'
                """)) {
            assertThat(legacyIndex.next()).as("legacy device-scoped index is dropped").isFalse();
        }
        try (ResultSet batchChecksum = statement.executeQuery("""
                SELECT indexname FROM pg_indexes
                WHERE tablename = 'import_batches' AND indexname = 'uq_import_batches_app_source_checksum'
                """)) {
            assertThat(batchChecksum.next()).as("per-source batch checksum uniqueness exists").isTrue();
        }
        try (ResultSet evidencePk = statement.executeQuery("""
                SELECT column_name, ordinal_position FROM information_schema.key_column_usage
                WHERE constraint_name = 'pk_punch_import_evidence'
                ORDER BY ordinal_position
                """)) {
            assertThat(evidencePk.next()).isTrue();
            assertThat(evidencePk.getString(1)).isEqualTo("punch_id");
            assertThat(evidencePk.next()).isTrue();
            assertThat(evidencePk.getString(1)).isEqualTo("batch_id");
            assertThat(evidencePk.next()).isTrue();
            assertThat(evidencePk.getString(1)).isEqualTo("row_number");
            assertThat(evidencePk.next()).isFalse();
        }
        try (ResultSet sourceFk = statement.executeQuery("""
                SELECT constraint_name FROM information_schema.table_constraints
                WHERE table_name = 'punch_records' AND constraint_type = 'FOREIGN KEY'
                """)) {
            assertThat(sourceFk.next()).isTrue();
            assertThat(sourceFk.getString(1)).isEqualTo("fk_punch_records_source");
        }
        try (ResultSet batchFk = statement.executeQuery("""
                SELECT constraint_name FROM information_schema.table_constraints
                WHERE table_name = 'import_batches' AND constraint_type = 'FOREIGN KEY'
                """)) {
            assertThat(batchFk.next()).isTrue();
            assertThat(batchFk.getString(1)).isEqualTo("fk_import_batches_source");
        }
    }

    @Test
    void deviceNamesThatDifferOnlyByInnerWhitespaceResolveToOneSource() throws Exception {
        PostgreSQLContainer<?> postgres = com.bemo.hr.PostgresIntegrationTest.POSTGRES;
        String baseUrl = postgres.getJdbcUrl();
        String hostPart = baseUrl.substring(0, baseUrl.lastIndexOf('/'));
        String migrationDb = "bemo_v92_normalization_test";

        try (Connection admin = DriverManager.getConnection(
                hostPart + "/postgres", postgres.getUsername(), postgres.getPassword())) {
            try (Statement statement = admin.createStatement()) {
                statement.execute("DROP DATABASE IF EXISTS " + migrationDb + " WITH (FORCE)");
                statement.execute("CREATE DATABASE " + migrationDb);
            }
        }

        try (Connection connection = DriverManager.getConnection(
                hostPart + "/" + migrationDb, postgres.getUsername(), postgres.getPassword())) {
            apply(connection, PRE_V92);
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                        INSERT INTO apps (id, code, name) VALUES ('app2', 'APP-2', 'Normalization App')
                        """);
                statement.execute("""
                        INSERT INTO import_batches
                            (id, app_id, checksum, file_name, device_name, status, total_rows, imported_rows, error_rows, imported_by, imported_at)
                        VALUES
                            ('n1', 'app2', 'c1', 'a.csv', '  Gate  One  ', 'COMPLETED', 1, 1, 0, 'tester', CURRENT_TIMESTAMP),
                            ('n2', 'app2', 'c2', 'b.csv', 'Gate_One', 'COMPLETED', 1, 1, 0, 'tester', CURRENT_TIMESTAMP)
                        """);
            }
            apply(connection, V92);

            try (Statement statement = connection.createStatement()) {
                try (ResultSet fileSources = statement.executeQuery("""
                        SELECT COUNT(*) FROM biometric_sources
                        WHERE app_id = 'app2' AND source_type = 'FILE_DEVICE'
                        """)) {
                    fileSources.next();
                    assertThat(fileSources.getInt(1)).as("inner whitespace and underscores are one identity").isEqualTo(1);
                }
                try (ResultSet bothBatches = statement.executeQuery("""
                        SELECT COUNT(DISTINCT b.source_id)
                        FROM import_batches b
                        WHERE b.app_id = 'app2'
                        """)) {
                    bothBatches.next();
                    assertThat(bothBatches.getInt(1)).as("both batches map to the same source").isEqualTo(1);
                }
            }
        } finally {
            try (Connection admin = DriverManager.getConnection(
                    hostPart + "/postgres", postgres.getUsername(), postgres.getPassword())) {
                try (Statement statement = admin.createStatement()) {
                    statement.execute("DROP DATABASE IF EXISTS " + migrationDb + " WITH (FORCE)");
                }
            }
        }
    }
}
