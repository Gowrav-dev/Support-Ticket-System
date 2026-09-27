package com.helpdesk.config;

import com.helpdesk.util.PasswordUtil;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Database Connection Manager:
 * Connects to MySQL, or uses embedded H2 database as a seamless fallback.
 * Automatically initializes tables and default demo accounts.
 */
public class Database {

    private static String dbUrl = "jdbc:mysql://localhost:3306/ai_helpdesk?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static String dbUser = "root";
    private static String dbPassword = "";
    private static boolean useFallback = false;

    public static void setTestUrl(String url) {
        dbUrl = url;
    }

    public static Connection getConnection() throws SQLException {
        if (useFallback) {
            return DriverManager.getConnection("jdbc:h2:mem:ai_helpdesk;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        }

        String envUrl = System.getenv("DB_URL");
        String envUser = System.getenv("DB_USER");
        String envPass = System.getenv("DB_PASSWORD");

        String url = (envUrl != null && !envUrl.isBlank()) ? envUrl : dbUrl;
        String user = (envUser != null && !envUser.isBlank()) ? envUser : dbUser;
        String pass = (envPass != null) ? envPass : dbPassword;

        try {
            return DriverManager.getConnection(url, user, pass);
        } catch (SQLException e) {
            // Fall back to in-memory H2 database
            useFallback = true;
            Connection conn = DriverManager.getConnection("jdbc:h2:mem:ai_helpdesk;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
            createTables(conn, true);
            return conn;
        }
    }

    public static void initDatabase() {
        try (Connection conn = getConnection()) {
            boolean isH2 = conn.getMetaData().getURL().startsWith("jdbc:h2:");
            createTables(conn, isH2);
            seedDemoUsers(conn);
            System.out.println("[Database] Tables and seed accounts are ready.");
        } catch (Exception e) {
            System.err.println("[Database] Notice: " + e.getMessage());
        }
    }

    private static void createTables(Connection conn, boolean isH2) {
        try (Statement stmt = conn.createStatement()) {
            // 1. Users Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(50) UNIQUE NOT NULL,
                    password_hash VARCHAR(255) NOT NULL,
                    full_name VARCHAR(100) NOT NULL,
                    role VARCHAR(20) NOT NULL DEFAULT 'USER',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
            """);

            // 2. Tickets Table
            if (isH2) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS tickets (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        user_name VARCHAR(100) NOT NULL,
                        title VARCHAR(200) NOT NULL,
                        description CLOB NOT NULL,
                        category VARCHAR(50) DEFAULT 'OTHER',
                        priority VARCHAR(20) DEFAULT 'MEDIUM',
                        ai_summary CLOB,
                        ai_solution CLOB,
                        status VARCHAR(20) DEFAULT 'OPEN',
                        assigned_to VARCHAR(100) DEFAULT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    );
                """);
            } else {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS tickets (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        user_name VARCHAR(100) NOT NULL,
                        title VARCHAR(200) NOT NULL,
                        description TEXT NOT NULL,
                        category VARCHAR(50) DEFAULT 'OTHER',
                        priority VARCHAR(20) DEFAULT 'MEDIUM',
                        ai_summary TEXT,
                        ai_solution TEXT,
                        status VARCHAR(20) DEFAULT 'OPEN',
                        assigned_to VARCHAR(100) DEFAULT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    );
                """);
            }

            // Ensure assigned_to column exists if table existed previously without it
            try {
                stmt.execute("ALTER TABLE tickets ADD COLUMN IF NOT EXISTS assigned_to VARCHAR(100) DEFAULT NULL");
            } catch (Exception ignored) {
                // Some MySQL versions use different syntax; ignore if column exists
            }

        } catch (SQLException e) {
            System.err.println("[Database] Table creation error: " + e.getMessage());
        }
    }

    private static void seedDemoUsers(Connection conn) {
        String countSql = "SELECT COUNT(*) FROM users";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(countSql)) {
            if (rs.next() && rs.getInt(1) > 0) {
                return; // Already seeded
            }
        } catch (Exception ignored) {}

        String hash = PasswordUtil.hash("Password@123");
        String insertSql = "INSERT INTO users (username, password_hash, full_name, role) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
            // User
            stmt.setString(1, "john_doe");
            stmt.setString(2, hash);
            stmt.setString(3, "John Doe");
            stmt.setString(4, "USER");
            stmt.executeUpdate();

            // Support Staff 1
            stmt.setString(1, "alex_support");
            stmt.setString(2, hash);
            stmt.setString(3, "Alex Rivera");
            stmt.setString(4, "SUPPORT_STAFF");
            stmt.executeUpdate();

            // Support Staff 2
            stmt.setString(1, "sarah_support");
            stmt.setString(2, hash);
            stmt.setString(3, "Sarah Chen");
            stmt.setString(4, "SUPPORT_STAFF");
            stmt.executeUpdate();

            System.out.println("[Database] Default demo accounts created:");
            System.out.println("  User:          john_doe / Password@123");
            System.out.println("  Support Staff 1: alex_support / Password@123 (Alex Rivera)");
            System.out.println("  Support Staff 2: sarah_support / Password@123 (Sarah Chen)");
        } catch (SQLException e) {
            System.err.println("[Database] Seed users notice: " + e.getMessage());
        }
    }
}
