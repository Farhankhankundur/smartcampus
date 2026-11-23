package com.campus.model;

public class Booking extends BaseEntity {
    private int resourceId;
    private String bookedBy;
    private String slot;

    public Booking() { }
    public Booking(int id, int resourceId, String bookedBy, String slot) {
        this.id = id; this.resourceId = resourceId; this.bookedBy = bookedBy; this.slot = slot;
    }
    public int getResourceId() { return resourceId; }
    public String getBookedBy() { return bookedBy; }
    public String getSlot() { return slot; }
    @Override public void validate() {
        if (resourceId <= 0 || bookedBy == null || bookedBy.isBlank() || slot == null || slot.isBlank())
            throw new IllegalArgumentException("Choose a resource, enter your name and pick a time slot.");
    }
}
