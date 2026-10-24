package com.scc.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void initialize() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    userID INTEGER PRIMARY KEY AUTOINCREMENT,
                    fullname TEXT NOT NULL,
                    email TEXT NOT NULL UNIQUE,
                    username TEXT NOT NULL UNIQUE,
                    password TEXT NOT NULL,
                    user_role TEXT NOT NULL CHECK (user_role IN ('ADMIN','ORGANIZER','CUSTOMER')),
                    status TEXT NOT NULL DEFAULT 'PENDING',
                    dateRegistered TEXT DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // TODO: add the other ERD tables here (events, tickets, etc.)
            // Parent tables first so FOREIGN KEYs work.

            seedDefaultAdmin(conn);
            System.out.println("-> Database initialized");

        } catch (SQLException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void seedDefaultAdmin(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE user_role = 'ADMIN'")) {
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.executeUpdate("INSERT INTO users(fullname, email, username, password, user_role, status) "
                        + "VALUES('Administrator', 'admin@gmail.com', 'admin', 'admin123', 'ADMIN', 'APPROVED')");
                System.out.println("-> Default Admin created");
            }
        }
    }
}