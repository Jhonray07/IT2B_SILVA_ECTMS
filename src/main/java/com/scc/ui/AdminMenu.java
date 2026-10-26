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
            System.out.println("  [1] Manage Users");
            System.out.println("  [2] Manage Events");
            System.out.println("  [3] View All Tickets");
            System.out.println("  [4] Reports");
            System.out.println("  [5] Audit Logs");
            System.out.println("  [6] System Settings");
            System.out.println("  [0] Logout");
            System.out.println("------------------------------------------");
            System.out.print("Enter choice: ");

            String input = sc.nextLine().trim();

            switch (input) {
                case "1" -> manageUsers(admin);
                case "2" -> System.out.println("Manage Events is not available yet.");
                case "3" -> System.out.println("View All Tickets is not available yet.");
                case "4" -> System.out.println("Reports is not available yet.");
                case "5" -> System.out.println("Audit Logs is not available yet.");
                case "6" -> System.out.println("System Settings is not available yet.");
                case "0" -> {
                    System.out.println("Logging out... Goodbye, " + admin.fullname() + "!");
                    loggedIn = false;
                }
                default -> System.out.println("Invalid choice. Please enter a number from 0 to 6.");
            }
        }
    }

    // ---------- Manage Users submenu (CRUD) ----------

    private void manageUsers(User admin) {
        boolean back = false;

        while (!back) {
            System.out.println("\n------------- MANAGE USERS -------------");
            System.out.println("  READ");
            System.out.println("  [1] View All Users");
            System.out.println("  [2] View Pending Registrations");
            System.out.println("  UPDATE");
            System.out.println("  [3] Approve User Account");
            System.out.println("  [4] Update User Role & Status");
            System.out.println("  [0] Back");
            System.out.println("----------------------------------------");
            System.out.print("Enter choice: ");

            String input = sc.nextLine().trim();

            switch (input) {
                case "1" -> viewAllUsers();
                case "2" -> viewPendingUsers();
                case "3" -> approveUser();
                case "4" -> updateUser(admin);
                case "0" -> back = true;
                default -> System.out.println("Invalid choice. Please enter a number from 0 to 4.");
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
            int id = readIntInput("Enter Target User ID to approve (0 to cancel): ");

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

    private void updateUser(User currentAdmin) {
        System.out.println("\n--- UPDATE USER ROLE & STATUS ---");
        List<User> users = userDao.getAllUsers();
        printUserTable(users);

        if (users.isEmpty()) {
            return;
        }

        int targetId;
        while (true) {
            targetId = readIntInput("Enter Target User ID to update (0 to cancel): ");

            if (targetId == 0) {
                System.out.println("Update cancelled.");
                return;
            }

            if (targetId == currentAdmin.userID()) {
                System.out.println("WARNING: Modifying the currently active admin account is restricted.");
                continue;
            }

            final int id = targetId;
            if (users.stream().noneMatch(u -> u.userID() == id)) {
                System.out.println("Invalid! User ID " + id + " does not exist.");
                continue;
            }
            break;
        }

        String newRole = readValidRole();
        if (newRole == null) {
            System.out.println("Update cancelled.");
            return;
        }

        String newStatus = readValidStatus();
        if (newStatus == null) {
            System.out.println("Update cancelled.");
            return;
        }

        if (userDao.updateUserRoleAndStatus(targetId, newRole, newStatus)) {
            System.out.println("\n-> Success! User ID " + targetId + " updated to Role: "
                    + newRole + ", Status: " + newStatus + ".");
        } else {
            System.out.println("-> Update failed. Please try again.");
        }
    }

    // ---------- Input validation wrappers ----------

    private int readIntInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid! Please enter a number (digits only).");
            }
        }
    }

    /** Returns a valid role in uppercase, or null if the admin entered 0 to cancel. */
    private String readValidRole() {
        while (true) {
            System.out.print("Enter new role (ADMIN, ORGANIZER, CUSTOMER) (0 to cancel): ");
            String input = sc.nextLine().trim().toUpperCase();

            if (input.equals("0")) {
                return null;
            }
            if (input.equals("ADMIN") || input.equals("ORGANIZER") || input.equals("CUSTOMER")) {
                return input;
            }
            System.out.println("Invalid role! Choose ADMIN, ORGANIZER, or CUSTOMER.");
        }
    }

    /** Returns a valid status in uppercase, or null if the admin entered 0 to cancel. */
    private String readValidStatus() {
        while (true) {
            System.out.print("Enter new status (APPROVED, PENDING) (0 to cancel): ");
            String input = sc.nextLine().trim().toUpperCase();

            if (input.equals("0")) {
                return null;
            }
            if (input.equals("APPROVED") || input.equals("PENDING")) {
                return input;
            }
            System.out.println("Invalid status! Choose APPROVED or PENDING.");
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