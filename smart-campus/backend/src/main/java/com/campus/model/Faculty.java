package com.campus.model;

public class Faculty extends Person {
    private String department;

    public Faculty() { }
    public Faculty(int id, String name, String email, String department) {
        super(id, name, email);
        this.department = department;
    }
    public String getDepartment() { return department; }
    @Override public String getRole() { return "Faculty"; }
}
