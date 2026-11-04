package com.scc.model;

public record Event(int eventID, String title, String venue, String eventDate,
                    double price, int capacity, int available) {
}