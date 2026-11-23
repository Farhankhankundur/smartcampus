package com.campus.model;

public class CampusEvent extends BaseEntity {
    private String title;
    private String eventDate;
    private String venue;

    public CampusEvent() { }
    public CampusEvent(int id, String title, String eventDate, String venue) {
        this.id = id; this.title = title; this.eventDate = eventDate; this.venue = venue;
    }
    public String getTitle() { return title; }
    public String getEventDate() { return eventDate; }
    public String getVenue() { return venue; }
    @Override public void validate() {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Event title is required.");
    }
}
