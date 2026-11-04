package com.scc.ui;

import com.scc.dao.EventDao;
import com.scc.dao.UserDao;
import com.scc.model.OrganizerEvent;
import com.scc.model.Ticket;
import com.scc.model.User;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

public class OrganizerMenu {

    private final Scanner sc;
    private final UserDao userDao;
    private final EventDao eventDao = new EventDao();

    public OrganizerMenu(Scanner sc, UserDao userDao) {
        this.sc = sc;
        this.userDao = userDao;
    }

    public void display(User organizer) {
        while (true) {
            System.out.println("\n==========================================");
            System.out.println("  ORGANIZER MENU | Logged in: " + organizer.fullname());
            System.out.println("==========================================");
            System.out.println("  [1] Create Event");
            System.out.println("  [2] View My Events");
            System.out.println("  [3] Update Event");
            System.out.println("  [4] Cancel Event");
            System.out.println("  [5] Sales Report & Attendee List");
            System.out.println("  [0] Logout");
            System.out.println("------------------------------------------");
            System.out.print("Enter choice: ");
            String input = sc.nextLine().trim();

            switch (input) {
                case "1" -> createEvent(organizer);
                case "2" -> {
                    System.out.println("\n--- MY EVENTS ---");
                    printEventTable(eventDao.getEventsByOrganizer(organizer.userID()));
                }
                case "3" -> updateEvent(organizer);
                case "4" -> cancelEvent(organizer);
                case "5" -> salesReport(organizer);
                case "0" -> {
                    System.out.println("Logging out... Goodbye, " + organizer.fullname() + "!");
                    return;
                }
                default -> System.out.println("Invalid choice. Please enter a number from 0 to 5.");
            }
        }
    }


    private void createEvent(User organizer) {
        System.out.println("\n--- CREATE EVENT ---");

        String title = readRequiredText("Event title (0 to cancel): ");
        if (title == null) {
            System.out.println("Creation cancelled.");
            return;
        }

        String venue = readRequiredText("Venue / Link (0 to cancel): ");
        if (venue == null) {
            System.out.println("Creation cancelled.");
            return;
        }

        String date = readFutureDate("Event date YYYY-MM-DD (0 to cancel): ");
        if (date == null) {
            System.out.println("Creation cancelled.");
            return;
        }

        double price = readPrice("Ticket price (0 for free): ");

        int capacity = readIntInput("Maximum capacity (0 to cancel): ");
        while (capacity < 0) {
            System.out.println("Invalid! Capacity must be a positive number.");
            capacity = readIntInput("Maximum capacity (0 to cancel): ");
        }
        if (capacity == 0) {
            System.out.println("Creation cancelled.");
            return;
        }

        int eventId = eventDao.createEvent(organizer.userID(), title, venue, date, price, capacity);
        if (eventId > 0) {
            System.out.println("\n-> Success! Event #" + eventId + " '" + title + "' has been created.");
        } else {
            System.out.println("-> Failed to create the event.");
        }
    }


    private void updateEvent(User organizer) {
        System.out.println("\n--- UPDATE EVENT ---");
        List<OrganizerEvent> events = eventDao.getEventsByOrganizer(organizer.userID());
        printEventTable(events);

        if (events.isEmpty()) {
            return;
        }

        OrganizerEvent ev = pickEvent(events, "Enter Event ID to update (0 to cancel): ", true);
        if (ev == null) {
            return;
        }

        if (!isFutureDate(ev.eventDate())) {
            System.out.println("This event has already started or the date is not valid. It can no longer be updated.");
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
                // fall through to message
            }
            System.out.println("Invalid! Enter a whole number greater than 0.");
        }

        if (eventDao.updateEvent(organizer.userID(), ev.eventID(), title, venue, date, price, capacity)) {
            System.out.println("\n-> Success! Event #" + ev.eventID() + " has been updated.");
        } else {
            System.out.println("-> Update failed.");
        }
    }


    private void cancelEvent(User organizer) {
        System.out.println("\n--- CANCEL EVENT ---");
        List<OrganizerEvent> events = eventDao.getEventsByOrganizer(organizer.userID());
        printEventTable(events);

        if (events.isEmpty()) {
            return;
        }

        OrganizerEvent ev = pickEvent(events, "Enter Event ID to cancel (0 to go back): ", true);
        if (ev == null) {
            return;
        }

        System.out.print("Cancel '" + ev.title() + "'? All its tickets will be voided. (Y/N): ");
        String confirm = sc.nextLine().trim();
        if (!confirm.equalsIgnoreCase("Y")) {
            System.out.println("Cancellation aborted.");
            return;
        }

        int voided = eventDao.cancelEvent(organizer.userID(), ev.eventID());
        if (voided >= 0) {
            System.out.println("\n-> Event #" + ev.eventID() + " cancelled. Tickets voided: " + voided);
        } else {
            System.out.println("-> Cancellation failed.");
        }
    }


    private void salesReport(User organizer) {
        System.out.println("\n--- EVENT SALES SUMMARY ---");
        List<OrganizerEvent> events = eventDao.getEventsByOrganizer(organizer.userID());
        printSalesTable(events);

        if (events.isEmpty()) {
            return;
        }

        int id = readIntInput("Enter Event ID to view attendees (0 to go back): ");
        if (id == 0) {
            return;
        }

        boolean owned = events.stream().anyMatch(e -> e.eventID() == id);
        if (!owned) {
            System.out.println("Invalid! Event ID " + id + " is not one of your events.");
            return;
        }

        System.out.println("\n--- ATTENDEES FOR EVENT #" + id + " ---");
        printAttendeeTable(eventDao.getAttendeesByEvent(organizer.userID(), id));
    }


    private OrganizerEvent pickEvent(List<OrganizerEvent> events, String prompt, boolean activeOnly) {
        while (true) {
            int id = readIntInput(prompt);
            if (id == 0) {
                System.out.println("Cancelled.");
                return null;
            }

            OrganizerEvent match = null;
            for (OrganizerEvent e : events) {
                if (e.eventID() == id) {
                    match = e;
                    break;
                }
            }

            if (match == null) {
                System.out.println("Invalid! Event ID " + id + " is not one of your events.");
                continue;
            }
            if (activeOnly && match.status().equals("CANCELLED")) {
                System.out.println("That event is already cancelled.");
                continue;
            }
            return match;
        }
    }

    private String readRequiredText(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            if (input.equals("0")) {
                return null;
            }
            if (input.isEmpty()) {
                System.out.println("Invalid! This field cannot be empty.");
                continue;
            }
            return input;
        }
    }

    private String readFutureDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            if (input.equals("0")) {
                return null;
            }
            if (isFutureDate(input)) {
                return input;
            }
            System.out.println("Invalid! Use the format YYYY-MM-DD and a date after today.");
        }
    }

    private boolean isFutureDate(String date) {
        try {
            return LocalDate.parse(date).isAfter(LocalDate.now());
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private double readPrice(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            try {
                double price = Double.parseDouble(input);
                if (price >= 0) {
                    return price;
                }
            } catch (NumberFormatException ignored) {
            }
            System.out.println("Invalid! Enter a price of 0 or more (numbers only).");
        }
    }

    private String readKeep(String label, String current) {
        System.out.print(label + " [" + current + "]: ");
        String input = sc.nextLine().trim();
        return input.isEmpty() ? current : input;
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


    private void printEventTable(List<OrganizerEvent> events) {
        if (events.isEmpty()) {
            System.out.println("You have no events yet.");
            return;
        }

        String format = "| %-4s | %-22s | %-14s | %-10s | %9s | %-7s | %-9s |%n";
        String border = "+------+------------------------+----------------+------------+-----------+---------+-----------+";

        System.out.println(border);
        System.out.printf(format, "ID", "Title", "Venue", "Date", "Price", "Sold", "Status");
        System.out.println(border);
        for (OrganizerEvent e : events) {
            System.out.printf(format, e.eventID(), e.title(), e.venue(), e.eventDate(),
                    String.format("P%.2f", e.price()), e.sold() + "/" + e.capacity(), e.status());
        }
        System.out.println(border);
        System.out.println("Total: " + events.size());
    }

    private void printSalesTable(List<OrganizerEvent> events) {
        if (events.isEmpty()) {
            System.out.println("You have no events yet.");
            return;
        }

        String format = "| %-4s | %-22s | %6s | %9s | %12s | %-9s |%n";
        String border = "+------+------------------------+--------+-----------+--------------+-----------+";

        double totalRevenue = 0;
        int totalSold = 0;

        System.out.println(border);
        System.out.printf(format, "ID", "Event", "Sold", "Remaining", "Revenue", "Status");
        System.out.println(border);
        for (OrganizerEvent e : events) {
            System.out.printf(format, e.eventID(), e.title(), e.sold(), e.remaining(),
                    String.format("P%.2f", e.revenue()), e.status());
            totalRevenue += e.revenue();
            totalSold += e.sold();
        }
        System.out.println(border);
        System.out.println("Total tickets sold: " + totalSold);
        System.out.printf("Total revenue (PAID tickets): P%.2f%n", totalRevenue);
    }

    private void printAttendeeTable(List<Ticket> tickets) {
        if (tickets.isEmpty()) {
            System.out.println("No tickets have been booked for this event yet.");
            return;
        }

        String format = "| %-6s | %-14s | %-3s | %10s | %-10s |%n";
        String border = "+--------+----------------+-----+------------+------------+";

        System.out.println(border);
        System.out.printf(format, "Ticket", "Customer", "Qty", "Total", "Status");
        System.out.println(border);
        for (Ticket t : tickets) {
            System.out.printf(format, t.ticketID(), t.customerUsername(), t.quantity(),
                    String.format("P%.2f", t.totalAmount()), t.status());
        }
        System.out.println(border);
        System.out.println("Total: " + tickets.size());
    }
}