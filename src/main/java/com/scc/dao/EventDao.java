package com.scc.dao;

import com.scc.model.AdminEvent;
import com.scc.model.OrganizerEvent;
import com.scc.model.Ticket;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class EventDao {

    private final ActivityLogDao activityLogDao = new ActivityLogDao();

    private static final String SOLD_EXPR =
            "COALESCE((SELECT SUM(t.quantity) FROM tickets t "
                    + "WHERE t.event_id = e.eventID AND t.status <> 'CANCELLED'), 0)";

    public EventDao() {
        ensureColumns();
    }


    private void ensureColumns() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            boolean hasOrganizer = false;
            boolean hasStatus = false;
            try (ResultSet rs = stmt.executeQuery("PRAGMA table_info(events)")) {
                while (rs.next()) {
                    String col = rs.getString("name");
                    if (col.equalsIgnoreCase("organizer_id")) hasOrganizer = true;
                    if (col.equalsIgnoreCase("status")) hasStatus = true;
                }
            }
            if (!hasOrganizer) {
                stmt.execute("ALTER TABLE events ADD COLUMN organizer_id INTEGER");
            }
            if (!hasStatus) {
                stmt.execute("ALTER TABLE events ADD COLUMN status TEXT DEFAULT 'ACTIVE'");
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }


    public int createEvent(int organizerId, String title, String venue, String date,
                           double price, int capacity) {
        String sql = "INSERT INTO events(title, venue, event_date, price, capacity, organizer_id, status) "
                + "VALUES(?, ?, ?, ?, ?, ?, 'ACTIVE')";
        int eventId = 0;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, title);
            pstmt.setString(2, venue);
            pstmt.setString(3, date);
            pstmt.setDouble(4, price);
            pstmt.setInt(5, capacity);
            pstmt.setInt(6, organizerId);
            pstmt.executeUpdate();
            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    eventId = keys.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return 0;
        }

        if (eventId > 0) {
            activityLogDao.logAction(organizerId, "EVENT_CREATED",
                    "Organizer " + organizerId + " created Event #" + eventId + " '" + title
                            + "' on " + date + " (capacity " + capacity + ", P"
                            + String.format("%.2f", price) + ")");
        }
        return eventId;
    }


    public List<OrganizerEvent> getEventsByOrganizer(int organizerId) {
        String sql = "SELECT e.eventID, e.title, e.venue, e.event_date, e.price, e.capacity, "
                + "COALESCE(e.status, 'ACTIVE') AS status, "
                + SOLD_EXPR + " AS sold, "
                + "COALESCE((SELECT SUM(t.total_amount) FROM tickets t "
                + "WHERE t.event_id = e.eventID AND t.status = 'PAID'), 0) AS revenue "
                + "FROM events e WHERE e.organizer_id = ? ORDER BY e.eventID";
        List<OrganizerEvent> events = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, organizerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    events.add(new OrganizerEvent(
                            rs.getInt("eventID"),
                            rs.getString("title"),
                            rs.getString("venue"),
                            rs.getString("event_date"),
                            rs.getDouble("price"),
                            rs.getInt("capacity"),
                            rs.getString("status"),
                            rs.getInt("sold"),
                            rs.getDouble("revenue")));
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return events;
    }


    public List<AdminEvent> getAllEventsForAdmin() {
        String sql = "SELECT e.eventID, e.title, COALESCE(u.username, '(none)') AS organizer, "
                + "e.venue, e.event_date, e.price, e.capacity, "
                + "COALESCE(e.status, 'ACTIVE') AS status, " + SOLD_EXPR + " AS sold "
                + "FROM events e LEFT JOIN users u ON u.userID = e.organizer_id "
                + "ORDER BY e.eventID";
        List<AdminEvent> events = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                events.add(new AdminEvent(
                        rs.getInt("eventID"),
                        rs.getString("title"),
                        rs.getString("organizer"),
                        rs.getString("venue"),
                        rs.getString("event_date"),
                        rs.getDouble("price"),
                        rs.getInt("capacity"),
                        rs.getString("status"),
                        rs.getInt("sold")));
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return events;
    }


    public List<Ticket> getAttendeesByEvent(int organizerId, int eventId) {
        String sql = "SELECT t.ticketID, t.event_id, e.title, t.customer_id, u.username, "
                + "t.quantity, t.total_amount, t.status, t.purchased_at "
                + "FROM tickets t "
                + "JOIN events e ON e.eventID = t.event_id "
                + "JOIN users u ON u.userID = t.customer_id "
                + "WHERE t.event_id = ? AND e.organizer_id = ? ORDER BY t.ticketID";
        List<Ticket> tickets = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, eventId);
            pstmt.setInt(2, organizerId);
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

    public boolean updateEvent(int organizerId, int eventId, String title, String venue,
                               String date, double price, int capacity) {
        return doUpdate(organizerId, organizerId, "Organizer", eventId, title, venue, date, price, capacity);
    }

    // Admin: can update any event.
    public boolean adminUpdateEvent(int adminId, int eventId, String title, String venue,
                                    String date, double price, int capacity) {
        return doUpdate(null, adminId, "Admin", eventId, title, venue, date, price, capacity);
    }

    private boolean doUpdate(Integer ownerId, int actorId, String actorRole, int eventId,
                             String title, String venue, String date, double price, int capacity) {
        String checkSql = "SELECT e.event_date, e.price, COALESCE(e.status, 'ACTIVE') AS status, "
                + SOLD_EXPR + " AS sold FROM events e WHERE e.eventID = ?"
                + (ownerId != null ? " AND e.organizer_id = ?" : "");
        String updateSql = "UPDATE events SET title = ?, venue = ?, event_date = ?, price = ?, capacity = ? "
                + "WHERE eventID = ?" + (ownerId != null ? " AND organizer_id = ?" : "");
        String oldDate;
        double oldPrice;

        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
                pstmt.setInt(1, eventId);
                if (ownerId != null) {
                    pstmt.setInt(2, ownerId);
                }
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println(" -> Event not found" + (ownerId != null ? " or not yours." : "."));
                        return false;
                    }
                    if (rs.getString("status").equals("CANCELLED")) {
                        System.out.println(" -> A CANCELLED event cannot be updated.");
                        return false;
                    }
                    int sold = rs.getInt("sold");
                    if (capacity < sold) {
                        System.out.println(" -> Capacity cannot be lower than tickets already sold (" + sold + ").");
                        return false;
                    }
                    oldDate = rs.getString("event_date");
                    oldPrice = rs.getDouble("price");
                }
            }

            try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                pstmt.setString(1, title);
                pstmt.setString(2, venue);
                pstmt.setString(3, date);
                pstmt.setDouble(4, price);
                pstmt.setInt(5, capacity);
                pstmt.setInt(6, eventId);
                if (ownerId != null) {
                    pstmt.setInt(7, ownerId);
                }
                if (pstmt.executeUpdate() == 0) {
                    return false;
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return false;
        }

        activityLogDao.logAction(actorId, "EVENT_UPDATED",
                actorRole + " " + actorId + " updated Event #" + eventId + " '" + title
                        + "' (date " + oldDate + " -> " + date + ", price P"
                        + String.format("%.2f", oldPrice) + " -> P" + String.format("%.2f", price) + ")");
        return true;
    }


    public int cancelEvent(int organizerId, int eventId) {
        return doCancel(organizerId, organizerId, "Organizer", eventId);
    }

    public int adminCancelEvent(int adminId, int eventId) {
        return doCancel(null, adminId, "Admin", eventId);
    }

    private int doCancel(Integer ownerId, int actorId, String actorRole, int eventId) {
        String checkSql = "SELECT title, COALESCE(status, 'ACTIVE') AS status FROM events WHERE eventID = ?"
                + (ownerId != null ? " AND organizer_id = ?" : "");
        String cancelSql = "UPDATE events SET status = 'CANCELLED' WHERE eventID = ?";
        String voidSql = "UPDATE tickets SET status = 'CANCELLED' WHERE event_id = ? AND status <> 'CANCELLED'";

        String title;
        int voided;

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
                    pstmt.setInt(1, eventId);
                    if (ownerId != null) {
                        pstmt.setInt(2, ownerId);
                    }
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            System.out.println(" -> Event not found" + (ownerId != null ? " or not yours." : "."));
                            return -1;
                        }
                        if (rs.getString("status").equals("CANCELLED")) {
                            conn.rollback();
                            System.out.println(" -> Event is already CANCELLED.");
                            return -1;
                        }
                        title = rs.getString("title");
                    }
                }

                try (PreparedStatement pstmt = conn.prepareStatement(cancelSql)) {
                    pstmt.setInt(1, eventId);
                    pstmt.executeUpdate();
                }

                try (PreparedStatement pstmt = conn.prepareStatement(voidSql)) {
                    pstmt.setInt(1, eventId);
                    voided = pstmt.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return -1;
        }

        activityLogDao.logAction(actorId, "EVENT_CANCELLED",
                actorRole + " " + actorId + " cancelled Event #" + eventId + " '" + title
                        + "'. Tickets voided: " + voided);
        return voided;
    }
}