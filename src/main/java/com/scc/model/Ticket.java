package com.scc.model;

public record Ticket(int ticketID, int eventID, String eventTitle, int customerID, String customerUsername,
                     int quantity, double totalAmount, String status, String purchasedAt) {
}