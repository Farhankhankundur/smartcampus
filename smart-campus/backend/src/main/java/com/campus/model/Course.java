package com.campus.model;

public class Course extends BaseEntity {
    private String code;
    private String title;
    private int credits;
    private int facultyId;

    public Course() { }
    public Course(int id, String code, String title, int credits, int facultyId) {
        this.id = id; this.code = code; this.title = title; this.credits = credits; this.facultyId = facultyId;
    }
    public String getCode() { return code; }
    public String getTitle() { return title; }
    public int getCredits() { return credits; }
    public int getFacultyId() { return facultyId; }
    @Override public void validate() {
        if (code == null || code.isBlank() || title == null || title.isBlank())
            throw new IllegalArgumentException("Course code and title are required.");
    }
}
