package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.List;
import java.util.Scanner;

public class AdminMenu {

    private final Scanner sc;
    private final UserDao userDao;

    public AdminMenu(Scanner sc, UserDao userDao) {
        this.sc = sc;
        this.userDao = userDao;
    }

    public void display(User admin) {
        boolean loggedIn = true;

        while (loggedIn) {
            System.out.println("\n==========================================");
            System.out.println("  EVENT & CONFERENCE TICKETING SYSTEM");
            System.out.println("  ADMIN MENU  | Logged in: " + admin.fullname());
            System.out.println("==========================================");
            System.out.println("  [1] View All System Users");
            System.out.println("  [2] View Pending Registrations");
            System.out.println("  [3] Approve User Account");
            System.out.println("  [4] Manage Events");
            System.out.println("  [5] View All Tickets");
            System.out.println("  [6] Reports");
            System.out.println("  [7] Audit Logs");
            System.out.println("  [8] System Settings");
            System.out.println("  [0] Logout");
            System.out.println("------------------------------------------");
            System.out.print("Enter choice: ");

            String input = sc.nextLine().trim();

            switch (input) {
                case "1" -> viewAllUsers();
                case "2" -> viewPendingUsers();
                case "3" -> approveUser();
                case "4" -> System.out.println("Manage Events is not available yet.");
                case "5" -> System.out.println("View All Tickets is not available yet.");
                case "6" -> System.out.println("Reports is not available yet.");
                case "7" -> System.out.println("Audit Logs is not available yet.");
                case "8" -> System.out.println("System Settings is not available yet.");
                case "0" -> {
                    System.out.println("Logging out... Goodbye, " + admin.fullname() + "!");
                    loggedIn = false;
                }
                default -> System.out.println("Invalid choice. Please enter a number from 0 to 8.");
            }
        }
    }

    private void viewAllUsers() {
        System.out.println("\n--- ALL SYSTEM USERS ---");
        printUserTable(userDao.getAllUsers());
    }

    private void viewPendingUsers() {
        System.out.println("\n--- PENDING REGISTRATIONS ---");
        printUserTable(userDao.getPendingUsers());
    }

    private void approveUser() {
        List<User> pending = userDao.getPendingUsers();

        System.out.println("\n--- APPROVE USER ACCOUNT ---");
        printUserTable(pending);

        if (pending.isEmpty()) {
            return;
        }

        while (true) {
            System.out.print("Enter Target User ID to approve (0 to cancel): ");
            String input = sc.nextLine().trim();

            int id;
            try {
                id = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid! Please enter a number.");
                continue;
            }

            if (id == 0) {
                System.out.println("Approval cancelled.");
                return;
            }

            boolean inPending = pending.stream().anyMatch(u -> u.userID() == id);
            if (!inPending) {
                System.out.println("Invalid! User ID " + id + " is not in the pending list.");
                continue;
            }

            if (userDao.approveUser(id)) {
                System.out.println("\n-> Success! User ID " + id + " has been APPROVED.");
            } else {
                System.out.println("-> Approval failed. Please try again.");
            }
            return;
        }
    }

    private void printUserTable(List<User> users) {
        if (users.isEmpty()) {
            System.out.println("No users to display.");
            return;
        }

        String format = "| %-5s | %-15s | %-12s | %-10s |%n";
        String border = "+-------+-----------------+--------------+------------+";

        System.out.println(border);
        System.out.printf(format, "ID", "Username", "Role", "Status");
        System.out.println(border);
        for (User u : users) {
            System.out.printf(format, u.userID(), u.username(), u.user_role(), u.status());
        }
        System.out.println(border);
        System.out.println("Total: " + users.size());
    }
}