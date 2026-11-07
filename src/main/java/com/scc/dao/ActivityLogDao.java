package com.scc.dao;

import com.scc.model.ActivityLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogDao {

    public boolean logAction(int userId, String actionType, String description) {
        String sql = "INSERT INTO activity_logs(user_id, action_type, description) VALUES(?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, actionType);
            pstmt.setString(3, description);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Audit log failed: " + e.getMessage());
            return false;
        }
    }

    // Deletes all logs and restarts the log ID counter at 1.
    public boolean clearAllLogs(int adminId) {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM activity_logs");
            // reset the AUTOINCREMENT counter so the next ID is 1
            stmt.executeUpdate("DELETE FROM sqlite_sequence WHERE name = 'activity_logs'");
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return false;
        }

        // becomes Log #1, so there is a record of who cleared the logs
        logAction(adminId, "LOGS_CLEARED", "Admin " + adminId + " cleared all system logs");
        return true;
    }

    public List<ActivityLog> getAllLogs() {
        String sql = "SELECT id, user_id, action_type, description, timestamp "
                + "FROM activity_logs ORDER BY id ASC";
        List<ActivityLog> logs = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                logs.add(mapLog(rs));
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return logs;
    }

    public List<ActivityLog> getLogsByUserId(int userId) {
        String sql = "SELECT id, user_id, action_type, description, timestamp "
                + "FROM activity_logs WHERE user_id = ? ORDER BY id ASC";
        List<ActivityLog> logs = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapLog(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return logs;
    }

    private ActivityLog mapLog(ResultSet rs) throws SQLException {
        return new ActivityLog(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getString("action_type"),
                rs.getString("description"),
                rs.getString("timestamp")
        );
    }
}