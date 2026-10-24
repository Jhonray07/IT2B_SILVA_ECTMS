package com.scc.model;

public record User(int userID, String fullname, String email, String username,
                   String password, String user_role, String status, String dateRegistered) {
    @Override
    public String toString() {
        return String.format("[%d] %s %s (%s) - Status: %s", userID, fullname, username, user_role, status);
    }
}