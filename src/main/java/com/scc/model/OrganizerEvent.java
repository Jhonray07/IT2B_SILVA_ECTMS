package com.scc.model;

public record OrganizerEvent(int eventID, String title, String venue, String eventDate,
                             double price, int capacity, String status, int sold, double revenue) {

    public int remaining() {
        return capacity - sold;
    }
}