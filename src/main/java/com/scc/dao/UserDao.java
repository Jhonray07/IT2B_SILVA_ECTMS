package com.scc.dao;

import com.scc.model.User;

import java.sql.*;

public class UserDao {

    public UserDao() {

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            defaultAdminGenerator(conn);
        } catch (SQLException e) {
            System.err.println("Error seeding default admin: " + e.getMessage());
        }

    }

    public void defaultAdminGenerator(Connection conn){
        String checksql = "SELECT COUNT(*) FROM users WHERE user_role = 'ADMIN'";
        String insertSql = "INSERT INTO users(fullname, email, username, password, user_role, status) VALUES('Administrator', 'admin@gmail.com', 'admin', 'admin123', 'ADMIN', 'APPROVED')";

        try(Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(checksql)){
            if(rs.next() && rs.getInt(1) == 0){
                stmt.executeUpdate(insertSql);
                System.out.println("-> Default Admin created");
            }
        }catch (SQLException e){
            System.err.println("Error seeding defualt admin: " + e.getMessage());
        }

    }

    public boolean registerUser(String fullname, String email, String username, String password, String user_role){

        String sql = "INSERT INTO users(fullname, email, username, password, user_role, status) VALUES(?, ?, ?, ?, ?, 'PENDING')";

        try(Connection conn = DatabaseConnection.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1, fullname);
            pstmt.setString(2, email);
            pstmt.setString(3, username);
            pstmt.setString(4, password);
            pstmt.setString(5, user_role);
            pstmt.executeUpdate();
            System.out.println("\n-> Successful Registration, Waiting for Approval");
            return true;
        }catch (SQLException e){
            System.err.println("Failed" + e.getMessage());
            return false;
        }
    }

    public boolean usernameExists(String username){
        String sql = "SELECT username FROM users WHERE username = ?";

        try(Connection conn = DatabaseConnection.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1, username);
            try(ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }catch (SQLException e){
                System.out.println("Database error: " + e.getMessage());
                return true;
        }
    }

    public User login(String username, String password){
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";

        try(Connection conn = DatabaseConnection.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            try(ResultSet rs = pstmt.executeQuery()){
                String status = rs.getString("status");
                if(rs.next()){
                    if("PENDING".equalsIgnoreCase(status)){
                        System.out.println(" -> Account is pending, Waiting for Approval");
                        return null;
                    }
                    return new User(
                            rs.getInt("userID"),
                            rs.getString("fullname"),
                            rs.getString("email"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("user_role"),
                            status,
                            rs.getString("dateRegistered")
                    );
                }else {
                    System.out.println(" -> Invalid username or password");
                }
            }
        }catch (SQLException e){
            System.out.println("Database error: " + e.getMessage());
        }
        return null;
    }
}
