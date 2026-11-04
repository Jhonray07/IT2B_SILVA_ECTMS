package com.scc.dao;

import com.scc.model.Event;
import com.scc.model.Ticket;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class TicketDao {

    private final ActivityLogDao activityLogDao = new ActivityLogDao();

    private static final String AVAILABLE_EXPR =
            "e.capacity - COALESCE((SELECT SUM(t.quantity) FROM tickets t "
                    + "WHERE t.event_id = e.eventID AND t.status <> 'CANCELLED'), 0)";

    private static final String TICKET_SELECT =
            "SELECT t.ticketID, t.event_id, e.title, t.customer_id, u.username, "
                    + "t.quantity, t.total_amount, t.status, t.purchased_at "
                    + "FROM tickets t "
                    + "JOIN events e ON e.eventID = t.event_id "
                    + "JOIN users u ON u.userID = t.customer_id ";

    public List<Event> getAllEvents() {
        String sql = "SELECT e.eventID, e.title, e.venue, e.event_date, e.price, e.capacity, "
                + AVAILABLE_EXPR + " AS available FROM events e "
                + "WHERE COALESCE(e.status, 'ACTIVE') <> 'CANCELLED' ORDER BY e.eventID";
        List<Event> events = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                events.add(new Event(
                        rs.getInt("eventID"),
                        rs.getString("title"),
                        rs.getString("venue"),
                        rs.getString("event_date"),
                        rs.getDouble("price"),
                        rs.getInt("capacity"),
                        rs.getInt("available")));
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return events;
    }


    public int purchaseTicket(int customerId, int eventId, int quantity) {
        String eventSql = "SELECT e.title, e.price, " + AVAILABLE_EXPR + " AS available "
                + "FROM events e WHERE e.eventID = ? AND COALESCE(e.status, 'ACTIVE') <> 'CANCELLED'";
        String insertSql = "INSERT INTO tickets(event_id, customer_id, quantity, total_amount, status) "
                + "VALUES(?, ?, ?, ?, 'PENDING')";

        String title;
        double total;
        int ticketId;

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement pstmt = conn.prepareStatement(eventSql)) {
                    pstmt.setInt(1, eventId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            System.out.println(" -> Event not found.");
                            return 0;
                        }
                        title = rs.getString("title");
                        int available = rs.getInt("available");
                        if (quantity < 1 || quantity > available) {
                            conn.rollback();
                            System.out.println(" -> Not enough tickets available. Only " + available + " left.");
                            return 0;
                        }
                        total = rs.getDouble("price") * quantity;
                    }
                }

                try (PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setInt(1, eventId);
                    pstmt.setInt(2, customerId);
                    pstmt.setInt(3, quantity);
                    pstmt.setDouble(4, total);
                    pstmt.executeUpdate();
                    try (ResultSet keys = pstmt.getGeneratedKeys()) {
                        ticketId = keys.next() ? keys.getInt(1) : 0;
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return 0;
        }

        activityLogDao.logAction(customerId, "TICKET_PURCHASED",
                "Processed purchase of " + quantity + " ticket(s) for '" + title
                        + "' (Ticket #" + ticketId + ") - Total: P" + String.format("%.2f", total));
        return ticketId;
    }


    public boolean updateTicketStatus(int adminId, int ticketId, String newStatus) {
        String selectSql = "SELECT status FROM tickets WHERE ticketID = ?";
        String updateSql = "UPDATE tickets SET status = ? WHERE ticketID = ?";
        String oldStatus;

        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
                pstmt.setInt(1, ticketId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println(" -> Ticket not found.");
                        return false;
                    }
                    oldStatus = rs.getString("status");
                }
            }

            if (oldStatus.equals(newStatus)) {
                System.out.println(" -> Ticket is already " + newStatus + ".");
                return false;
            }
            if (oldStatus.equals("CANCELLED")) {
                System.out.println(" -> A CANCELLED ticket cannot be changed.");
                return false;
            }

            try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                pstmt.setString(1, newStatus);
                pstmt.setInt(2, ticketId);
                if (pstmt.executeUpdate() == 0) {
                    return false;
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return false;
        }

        activityLogDao.logAction(adminId, "TICKET_UPDATED",
                "Admin " + adminId + " updated Ticket #" + ticketId
                        + " status from " + oldStatus + " to " + newStatus);
        return true;
    }

    public List<Ticket> getAllTickets() {
        return queryTickets(TICKET_SELECT + "ORDER BY t.ticketID", null);
    }

    public List<Ticket> getTicketsByCustomer(int customerId) {
        return queryTickets(TICKET_SELECT + "WHERE t.customer_id = ? ORDER BY t.ticketID", customerId);
    }

    private List<Ticket> queryTickets(String sql, Integer customerId) {
        List<Ticket> tickets = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (customerId != null) {
                pstmt.setInt(1, customerId);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    tickets.add(new Ticket(
                            rs.getInt("ticketID"),
                            rs.getInt("event_id"),
                            rs.getString("title"),
                            rs.getInt("customer_id"),
                            rs.getString("username"),
                            rs.getInt("quantity"),
                            rs.getDouble("total_amount"),
                            rs.getString("status"),
                            rs.getString("purchased_at")));
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return tickets;
    }
}