import java.sql.DriverManager;

public final class CreatePostgresDatabase {
    private CreatePostgresDatabase() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            throw new IllegalArgumentException("Usage: <admin-jdbc-url> <username> <password> <database-name>");
        }
        String databaseName = args[3];
        if (!databaseName.matches("[a-z][a-z0-9_]{0,62}")) {
            throw new IllegalArgumentException("Database name must be a safe lower-case PostgreSQL identifier.");
        }
        try (var connection = DriverManager.getConnection(args[0], args[1], args[2]);
             var lookup = connection.prepareStatement("select 1 from pg_database where datname = ?")) {
            lookup.setString(1, databaseName);
            try (var result = lookup.executeQuery()) {
                if (result.next()) {
                    System.out.println("Database already exists: " + databaseName);
                    return;
                }
            }
            try (var statement = connection.createStatement()) {
                statement.executeUpdate("create database \"" + databaseName + "\" encoding 'UTF8'");
            }
            System.out.println("Created UTF-8 database: " + databaseName);
        }
    }
}
