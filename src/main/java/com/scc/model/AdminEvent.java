package com.scc.model;

public record AdminEvent(int eventID, String title, String organizer, String venue, String eventDate,
                         double price, int capacity, String status, int sold) {
}