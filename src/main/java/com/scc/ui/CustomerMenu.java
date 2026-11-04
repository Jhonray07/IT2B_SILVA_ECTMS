package com.scc.ui;

import com.scc.dao.TicketDao;
import com.scc.dao.UserDao;
import com.scc.model.Event;
import com.scc.model.Ticket;
import com.scc.model.User;

import java.util.List;
import java.util.Scanner;

public class CustomerMenu {

    private final Scanner sc;
    private final UserDao userDao;
    private final TicketDao ticketDao = new TicketDao();

    public CustomerMenu(Scanner sc, UserDao userDao) {
        this.sc = sc;
        this.userDao = userDao;
    }

    public void display(User customer) {
        while (true) {
            System.out.println("\n==========================================");
            System.out.println("  CUSTOMER MENU | Logged in: " + customer.fullname());
            System.out.println("==========================================");
            System.out.println("  [1] Browse Events");
            System.out.println("  [2] Buy Ticket");
            System.out.println("  [3] My Tickets");
            System.out.println("  [0] Logout");
            System.out.print("Enter choice: ");
            String input = sc.nextLine().trim();

            switch (input) {
                case "1" -> {
                    System.out.println("\n--- UPCOMING EVENTS ---");
                    printEventTable(ticketDao.getAllEvents());
                }
                case "2" -> buyTicket(customer);
                case "3" -> {
                    System.out.println("\n--- MY TICKETS ---");
                    printMyTickets(ticketDao.getTicketsByCustomer(customer.userID()));
                }
                case "0" -> {
                    System.out.println("Logging out... Goodbye, " + customer.fullname() + "!");
                    return;
                }
                default -> System.out.println("Invalid choice. Please enter a number from 0 to 3.");
            }
        }
    }

    private void buyTicket(User customer) {
        List<Event> events = ticketDao.getAllEvents();

        System.out.println("\n--- BUY TICKET ---");
        printEventTable(events);

        if (events.isEmpty()) {
            return;
        }

        Event chosen;
        while (true) {
            int eventId = readIntInput("Enter Event ID to buy (0 to cancel): ");
            if (eventId == 0) {
                System.out.println("Purchase cancelled.");
                return;
            }
            Event match = null;
            for (Event e : events) {
                if (e.eventID() == eventId) {
                    match = e;
                    break;
                }
            }
            if (match == null) {
                System.out.println("Invalid! Event ID " + eventId + " does not exist.");
                continue;
            }
            if (match.available() == 0) {
                System.out.println("Sorry, that event is sold out.");
                continue;
            }
            chosen = match;
            break;
        }

        int quantity;
        while (true) {
            quantity = readIntInput("Enter number of tickets (0 to cancel): ");
            if (quantity == 0) {
                System.out.println("Purchase cancelled.");
                return;
            }
            if (quantity < 0 || quantity > chosen.available()) {
                System.out.println("Invalid! Enter a number from 1 to " + chosen.available() + ".");
                continue;
            }
            break;
        }

        int ticketId = ticketDao.purchaseTicket(customer.userID(), chosen.eventID(), quantity);
        if (ticketId > 0) {
            System.out.printf("%n-> Success! Ticket #%d reserved (PENDING). Total: P%.2f%n",
                    ticketId, chosen.price() * quantity);
        } else {
            System.out.println("-> Purchase failed. Please try again.");
        }
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

    private void printEventTable(List<Event> events) {
        if (events.isEmpty()) {
            System.out.println("No events to display.");
            return;
        }

        String format = "| %-4s | %-22s | %-14s | %-10s | %9s | %9s |%n";
        String border = "+------+------------------------+----------------+------------+-----------+-----------+";

        System.out.println(border);
        System.out.printf(format, "ID", "Title", "Venue", "Date", "Price", "Available");
        System.out.println(border);
        for (Event e : events) {
            System.out.printf(format, e.eventID(), e.title(), e.venue(), e.eventDate(),
                    String.format("P%.2f", e.price()), e.available());
        }
        System.out.println(border);
    }

    private void printMyTickets(List<Ticket> tickets) {
        if (tickets.isEmpty()) {
            System.out.println("You have no tickets yet.");
            return;
        }

        String format = "| %-6s | %-22s | %-3s | %10s | %-10s |%n";
        String border = "+--------+------------------------+-----+------------+------------+";

        System.out.println(border);
        System.out.printf(format, "Ticket", "Event", "Qty", "Total", "Status");
        System.out.println(border);
        for (Ticket t : tickets) {
            System.out.printf(format, t.ticketID(), t.eventTitle(), t.quantity(),
                    String.format("P%.2f", t.totalAmount()), t.status());
        }
        System.out.println(border);
    }
}