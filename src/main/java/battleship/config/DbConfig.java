package battleship.config;

import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.Properties;

public final class DbConfig {
    private static final Properties P = new Properties();
    private static boolean loaded = false;

    private DbConfig() {}

    private static synchronized void load() {
        if (loaded) return;
        loaded = true;
        Path p = Paths.get("config", "db.properties");
        if (!Files.exists(p)) p = Paths.get("config", "db.properties.example");
        if (Files.exists(p)) {
            try (InputStream in = Files.newInputStream(p)) {
                P.load(in);
            } catch (IOException ignored) {}
        }
    }

    public static Connection getConnection() throws SQLException {
        load();
        String url = System.getenv().getOrDefault("BATTLESHIP_DB_URL",
                P.getProperty("db.url", "jdbc:sqlserver://localhost:1433;databaseName=BattleShipDB;encrypt=true;trustServerCertificate=true"));
        String user = System.getenv().getOrDefault("BATTLESHIP_DB_USER", P.getProperty("db.user", "sa"));
        String pass = System.getenv().getOrDefault("BATTLESHIP_DB_PASSWORD", P.getProperty("db.password", ""));
        return DriverManager.getConnection(url, user, pass);
    }

    public static boolean ping() {
        try (Connection c = getConnection()) {
            return c.isValid(2);
        } catch (Exception e) {
            return false;
        }
    }
}
