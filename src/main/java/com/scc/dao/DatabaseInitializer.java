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

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS activity_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    action_type TEXT NOT NULL,
                    description TEXT NOT NULL,
                    timestamp TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
                    FOREIGN KEY (user_id) REFERENCES users(userID) ON DELETE CASCADE
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS events (
                    eventID INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    venue TEXT NOT NULL,
                    event_date TEXT NOT NULL,
                    price REAL NOT NULL CHECK (price >= 0),
                    capacity INTEGER NOT NULL CHECK (capacity > 0),
                    organizer_id INTEGER,
                    status TEXT DEFAULT 'ACTIVE'
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS tickets (
                    ticketID INTEGER PRIMARY KEY AUTOINCREMENT,
                    event_id INTEGER NOT NULL,
                    customer_id INTEGER NOT NULL,
                    quantity INTEGER NOT NULL CHECK (quantity > 0),
                    total_amount REAL NOT NULL,
                    status TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','PAID','CANCELLED')),
                    purchased_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
                    FOREIGN KEY (event_id) REFERENCES events(eventID) ON DELETE CASCADE,
                    FOREIGN KEY (customer_id) REFERENCES users(userID) ON DELETE CASCADE
                )
            """);

            seedDefaultAdmin(conn);
            seedSampleEvents(conn);
            System.out.println("-> Database initialized");

        } catch (SQLException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
            System.exit(1);
        }
    }

    // Deletes ALL data, then rebuilds tables + default admin + sample events.
    public static boolean resetDatabase() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            // child tables first, so foreign keys don't block the drop
            stmt.execute("DROP TABLE IF EXISTS tickets");
            stmt.execute("DROP TABLE IF EXISTS events");
            stmt.execute("DROP TABLE IF EXISTS activity_logs");
            stmt.execute("DROP TABLE IF EXISTS users");
        } catch (SQLException e) {
            System.out.println("Reset failed: " + e.getMessage());
            return false;
        }
        initialize();
        return true;
    }

    private static void seedSampleEvents(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM events")) {
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.executeUpdate("INSERT INTO events(title, venue, event_date, price, capacity) VALUES "
                        + "('Tech Summit 2026', 'Main Hall', '2026-12-05', 500.00, 100), "
                        + "('Music Fest', 'Open Grounds', '2026-12-12', 1200.00, 200), "
                        + "('Startup Pitch Night', 'Auditorium', '2026-12-20', 250.00, 50)");
                System.out.println("-> Sample events created");
            }
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