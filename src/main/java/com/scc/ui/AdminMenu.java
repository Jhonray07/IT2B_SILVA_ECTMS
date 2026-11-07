package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.DatabaseInitializer;
import com.scc.dao.EventDao;
import com.scc.dao.TicketDao;
import com.scc.dao.UserDao;
import com.scc.model.ActivityLog;
import com.scc.model.AdminEvent;
import com.scc.model.Ticket;
import com.scc.model.User;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

public class AdminMenu {

    private final Scanner sc;
    private final UserDao userDao;
    private final ActivityLogDao activityLogDao = new ActivityLogDao();
    private final TicketDao ticketDao = new TicketDao();
    private final EventDao eventDao = new EventDao();

    public AdminMenu(Scanner sc, UserDao userDao) {
        this.sc = sc;
        this.userDao = userDao;
    }

    public void display(User admin) {
        boolean loggedIn = true;

        while (loggedIn) {
            System.out.println("\n==========================================");
            System.out.println("  EVENT & CONFERENCE TICKETING SYSTEM ");
            System.out.println("  ADMIN MENU  | Logged in: " + admin.fullname());
            System.out.println("==========================================");
            System.out.println("  [1] Manage Users");
            System.out.println("  [2] Manage Events");
            System.out.println("  [3] Manage Tickets");
            System.out.println("  [4] Reports");
            System.out.println("  [5] View System Audit Logs");
            System.out.println("  [6] System Settings");
            System.out.println("  [0] Logout");
            System.out.println("------------------------------------------");
            System.out.print("Enter choice: ");

            String input = sc.nextLine().trim();

            switch (input) {
                case "1" -> manageUsers(admin);
                case "2" -> manageEvents(admin);
                case "3" -> manageTickets(admin);
                case "4" -> System.out.println("Reports is not available yet.");
                case "5" -> auditLogs(admin);
                case "6" -> {
                    if (systemSettings()) {
                        loggedIn = false; // force logout after reset
                    }
                }
                case "0" -> {
                    System.out.println("Logging out... Goodbye, " + admin.fullname() + "!");
                    loggedIn = false;
                }
                default -> System.out.println("Invalid choice. Please enter a number from 0 to 6.");
            }
        }
    }


    // returns true if the database was reset
    private boolean systemSettings() {
        System.out.println("\n----------- SYSTEM SETTINGS -----------");
        System.out.println("  [1] Reset Database");
        System.out.println("  [0] Back");
        System.out.println("---------------------------------------");
        System.out.print("Enter choice: ");

        String input = sc.nextLine().trim();
        switch (input) {
            case "1" -> {
                System.out.println("\nWARNING: This will DELETE all users, events, tickets, and logs.");
                System.out.println("Only the default admin and sample events will be restored.");
                System.out.print("Type RESET to confirm (anything else cancels): ");
                String confirm = sc.nextLine().trim();

                if (!confirm.equals("RESET")) {
                    System.out.println("Reset cancelled.");
                    return false;
                }
                if (DatabaseInitializer.resetDatabase()) {
                    System.out.println("\n-> Database reset complete. You will now be logged out.");
                    System.out.println("   Login with the default admin: admin / admin123");
                    return true;
                }
                System.out.println("-> Reset failed.");
            }
            case "0" -> { }
            default -> System.out.println("Invalid choice. Please enter 0 or 1.");
        }
        return false;
    }


    private void manageUsers(User admin) {
        boolean back = false;

        while (!back) {
            System.out.println("\n------------- MANAGE USERS -------------");
            System.out.println("  [1] View All Users");
            System.out.println("  [2] View Pending Registrations");
            System.out.println("  [3] Approve User Account");
            System.out.println("  [4] Update User Role & Status");
            System.out.println("  [0] Back");
            System.out.println("----------------------------------------");
            System.out.print("Enter choice: ");

            String input = sc.nextLine().trim();

            switch (input) {
                case "1" -> viewAllUsers();
                case "2" -> viewPendingUsers();
                case "3" -> approveUser(admin);
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

    private void approveUser(User admin) {
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

            if (userDao.approveUser(admin.userID(), id)) {
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

        if (userDao.updateUserRoleAndStatus(currentAdmin.userID(), targetId, newRole, newStatus)) {
            System.out.println("\n-> Success! User ID " + targetId + " updated to Role: "
                    + newRole + ", Status: " + newStatus + ".");
        } else {
            System.out.println("-> Update failed. Please try again.");
        }
    }


    private void manageEvents(User admin) {
        boolean back = false;

        while (!back) {
            System.out.println("\n------------- MANAGE EVENTS -------------");
            System.out.println("  [1] View All Events");
            System.out.println("  [2] Update Event");
            System.out.println("  [3] Cancel Event");
            System.out.println("  [0] Back");
            System.out.println("-----------------------------------------");
            System.out.print("Enter choice: ");

            String input = sc.nextLine().trim();

            switch (input) {
                case "1" -> {
                    System.out.println("\n--- ALL EVENTS ---");
                    printAdminEventTable(eventDao.getAllEventsForAdmin());
                }
                case "2" -> updateEvent(admin);
                case "3" -> cancelEvent(admin);
                case "0" -> back = true;
                default -> System.out.println("Invalid choice. Please enter a number from 0 to 3.");
            }
        }
    }

    private void updateEvent(User admin) {
        System.out.println("\n--- UPDATE EVENT ---");
        List<AdminEvent> events = eventDao.getAllEventsForAdmin();
        printAdminEventTable(events);

        if (events.isEmpty()) {
            return;
        }

        AdminEvent ev = pickEvent(events, "Enter Event ID to update (0 to cancel): ");
        if (ev == null) {
            return;
        }

        if (!isFutureDate(ev.eventDate())) {
            System.out.println("This event has already started or its date is not valid. It can no longer be updated.");
            return;
        }

        System.out.println("(Press Enter to keep the current value)");

        String title = readKeep("Title", ev.title());
        String venue = readKeep("Venue", ev.venue());

        String date;
        while (true) {
            date = readKeep("Date (YYYY-MM-DD)", ev.eventDate());
            if (date.equals(ev.eventDate()) || isFutureDate(date)) {
                break;
            }
            System.out.println("Invalid! Use YYYY-MM-DD and a future date.");
        }

        double price;
        while (true) {
            String in = readKeep("Price", String.format("%.2f", ev.price()));
            try {
                price = Double.parseDouble(in);
                if (price >= 0) {
                    break;
                }
            } catch (NumberFormatException ignored) {
            }
            System.out.println("Invalid! Enter a price of 0 or more.");
        }

        int capacity;
        while (true) {
            String in = readKeep("Capacity", String.valueOf(ev.capacity()));
            try {
                capacity = Integer.parseInt(in);
                if (capacity > 0) {
                    break;
                }
            } catch (NumberFormatException ignored) {
            }
            System.out.println("Invalid! Enter a whole number greater than 0.");
        }

        if (eventDao.adminUpdateEvent(admin.userID(), ev.eventID(), title, venue, date, price, capacity)) {
            System.out.println("\n-> Success! Event #" + ev.eventID() + " has been updated.");
        } else {
            System.out.println("-> Update failed.");
        }
    }

    private void cancelEvent(User admin) {
        System.out.println("\n--- CANCEL EVENT ---");
        List<AdminEvent> events = eventDao.getAllEventsForAdmin();
        printAdminEventTable(events);

        if (events.isEmpty()) {
            return;
        }

        AdminEvent ev = pickEvent(events, "Enter Event ID to cancel (0 to go back): ");
        if (ev == null) {
            return;
        }

        System.out.print("Cancel '" + ev.title() + "'? All its tickets will be voided. (Y/N): ");
        String confirm = sc.nextLine().trim();
        if (!confirm.equalsIgnoreCase("Y")) {
            System.out.println("Cancellation aborted.");
            return;
        }

        int voided = eventDao.adminCancelEvent(admin.userID(), ev.eventID());
        if (voided >= 0) {
            System.out.println("\n-> Event #" + ev.eventID() + " cancelled. Tickets voided: " + voided);
        } else {
            System.out.println("-> Cancellation failed.");
        }
    }

    private AdminEvent pickEvent(List<AdminEvent> events, String prompt) {
        while (true) {
            int id = readIntInput(prompt);
            if (id == 0) {
                System.out.println("Cancelled.");
                return null;
            }

            AdminEvent match = null;
            for (AdminEvent e : events) {
                if (e.eventID() == id) {
                    match = e;
                    break;
                }
            }

            if (match == null) {
                System.out.println("Invalid! Event ID " + id + " does not exist.");
                continue;
            }
            if (match.status().equals("CANCELLED")) {
                System.out.println("That event is already cancelled.");
                continue;
            }
            return match;
        }
    }

    private boolean isFutureDate(String date) {
        try {
            return LocalDate.parse(date).isAfter(LocalDate.now());
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private String readKeep(String label, String current) {
        System.out.print(label + " [" + current + "]: ");
        String input = sc.nextLine().trim();
        return input.isEmpty() ? current : input;
    }

    private void printAdminEventTable(List<AdminEvent> events) {
        if (events.isEmpty()) {
            System.out.println("No events to display.");
            return;
        }

        String format = "| %-4s | %-22s | %-12s | %-14s | %-10s | %9s | %-7s | %-9s |%n";
        String border = "+------+------------------------+--------------+----------------+------------+-----------+---------+-----------+";

        System.out.println(border);
        System.out.printf(format, "ID", "Title", "Organizer", "Venue", "Date", "Price", "Sold", "Status");
        System.out.println(border);
        for (AdminEvent e : events) {
            System.out.printf(format, e.eventID(), e.title(), e.organizer(), e.venue(), e.eventDate(),
                    String.format("P%.2f", e.price()), e.sold() + "/" + e.capacity(), e.status());
        }
        System.out.println(border);
        System.out.println("Total: " + events.size());
    }


    private void manageTickets(User admin) {
        boolean back = false;

        while (!back) {
            System.out.println("\n------------- MANAGE TICKETS -------------");
            System.out.println("  [1] View All Tickets");
            System.out.println("  [2] Update Ticket Status");
            System.out.println("  [0] Back");
            System.out.println("------------------------------------------");
            System.out.print("Enter choice: ");

            String input = sc.nextLine().trim();

            switch (input) {
                case "1" -> {
                    System.out.println("\n--- ALL TICKETS ---");
                    printTicketTable(ticketDao.getAllTickets());
                }
                case "2" -> updateTicketStatus(admin);
                case "0" -> back = true;
                default -> System.out.println("Invalid choice. Please enter a number from 0 to 2.");
            }
        }
    }

    private void updateTicketStatus(User admin) {
        System.out.println("\n--- UPDATE TICKET STATUS ---");
        List<Ticket> tickets = ticketDao.getAllTickets();
        printTicketTable(tickets);

        if (tickets.isEmpty()) {
            return;
        }

        int ticketId;
        while (true) {
            ticketId = readIntInput("Enter Ticket ID to update (0 to cancel): ");
            if (ticketId == 0) {
                System.out.println("Update cancelled.");
                return;
            }
            final int id = ticketId;
            if (tickets.stream().noneMatch(t -> t.ticketID() == id)) {
                System.out.println("Invalid! Ticket ID " + id + " does not exist.");
                continue;
            }
            break;
        }

        String newStatus = readValidTicketStatus();
        if (newStatus == null) {
            System.out.println("Update cancelled.");
            return;
        }

        if (ticketDao.updateTicketStatus(admin.userID(), ticketId, newStatus)) {
            System.out.println("\n-> Success! Ticket #" + ticketId + " is now " + newStatus + ".");
        } else {
            System.out.println("-> Update failed.");
        }
    }

    private String readValidTicketStatus() {
        while (true) {
            System.out.print("Enter new status (PAID, CANCELLED) (0 to cancel): ");
            String input = sc.nextLine().trim().toUpperCase();

            if (input.equals("0")) {
                return null;
            }
            if (input.equals("PAID") || input.equals("CANCELLED")) {
                return input;
            }
            System.out.println("Invalid status! Choose PAID or CANCELLED.");
        }
    }

    private void printTicketTable(List<Ticket> tickets) {
        if (tickets.isEmpty()) {
            System.out.println("No tickets to display.");
            return;
        }

        String format = "| %-6s | %-22s | %-12s | %-3s | %10s | %-10s |%n";
        String border = "+--------+------------------------+--------------+-----+------------+------------+";

        System.out.println(border);
        System.out.printf(format, "Ticket", "Event", "Customer", "Qty", "Total", "Status");
        System.out.println(border);
        for (Ticket t : tickets) {
            System.out.printf(format, t.ticketID(), t.eventTitle(), t.customerUsername(), t.quantity(),
                    String.format("P%.2f", t.totalAmount()), t.status());
        }
        System.out.println(border);
        System.out.println("Total: " + tickets.size());
    }


    private void auditLogs(User admin) {
        boolean back = false;

        while (!back) {
            System.out.println("\n----------- SYSTEM AUDIT LOGS -----------");
            System.out.println("  [1] View All System Logs");
            System.out.println("  [2] Filter Logs by User ID");
            System.out.println("  [3] Clear All Logs");
            System.out.println("  [0] Back");
            System.out.println("-----------------------------------------");
            System.out.print("Enter choice: ");

            String input = sc.nextLine().trim();

            switch (input) {
                case "1" -> {
                    System.out.println("\n--- ALL SYSTEM LOGS ---");
                    printLogTable(activityLogDao.getAllLogs());
                }
                case "2" -> filterLogsByUser();
                case "3" -> clearLogs(admin);
                case "0" -> back = true;
                default -> System.out.println("Invalid choice. Please enter a number from 0 to 3.");
            }
        }
    }

    private void filterLogsByUser() {
        int userId = readIntInput("Enter User ID to filter (0 to cancel): ");
        if (userId == 0) {
            System.out.println("Filter cancelled.");
            return;
        }
        System.out.println("\n--- LOGS FOR USER ID " + userId + " ---");
        printLogTable(activityLogDao.getLogsByUserId(userId));
    }

    private void clearLogs(User admin) {
        System.out.print("\nDelete ALL audit logs and restart log IDs at 1? (Y/N): ");
        String confirm = sc.nextLine().trim();

        if (!confirm.equalsIgnoreCase("Y")) {
            System.out.println("Clear logs cancelled.");
            return;
        }

        if (activityLogDao.clearAllLogs(admin.userID())) {
            System.out.println("\n-> All logs cleared. IDs will restart at 1.");
        } else {
            System.out.println("-> Failed to clear logs.");
        }
    }

    private void printLogTable(List<ActivityLog> logs) {
        if (logs.isEmpty()) {
            System.out.println("No logs to display.");
            return;
        }

        final int descWidth = 35;
        String format = "%-5s | %-8s | %-18s | %-35s | %-20s%n";
        String border = "-".repeat(5 + 3 + 8 + 3 + 18 + 3 + descWidth + 3 + 20);

        System.out.println(border);
        System.out.printf(format, "ID", "User ID", "Action Type", "Description", "Timestamp");
        System.out.println(border);
        for (ActivityLog log : logs) {
            List<String> lines = wrap(log.description(), descWidth);
            System.out.printf(format, log.id(), log.userId(), log.actionType(), lines.get(0), log.timestamp());
            for (int i = 1; i < lines.size(); i++) {
                System.out.printf(format, "", "", "", lines.get(i), "");
            }
        }
        System.out.println(border);
        System.out.println("Total: " + logs.size());
    }

    private List<String> wrap(String text, int width) {
        List<String> lines = new java.util.ArrayList<>();
        String rest = text.trim();

        while (rest.length() > width) {
            int cut = rest.lastIndexOf(' ', width);
            if (cut <= 0) {
                cut = width;
            }
            lines.add(rest.substring(0, cut).trim());
            rest = rest.substring(cut).trim();
        }
        lines.add(rest);
        return lines;
    }


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