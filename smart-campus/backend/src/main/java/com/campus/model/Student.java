package com.campus.model;

public class Student extends Person {
    private String department;
    private int semester;

    public Student() { }
    public Student(int id, String name, String email, String department, int semester) {
        super(id, name, email);
        this.department = department;
        this.semester = semester;
    }
    public String getDepartment() { return department; }
    public int getSemester() { return semester; }
    @Override public String getRole() { return "Student"; }
    @Override public void validate() {
        super.validate();
        if (semester < 1 || semester > 8) throw new IllegalArgumentException("Semester must be between 1 and 8.");
    }
}
