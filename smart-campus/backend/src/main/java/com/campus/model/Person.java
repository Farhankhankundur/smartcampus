package com.campus.model;

public abstract class Person extends BaseEntity {
    protected String name;
    protected String email;

    protected Person() { }
    protected Person(int id, String name, String email) { this.id = id; this.name = name; this.email = email; }

    public String getName() { return name; }
    public String getEmail() { return email; }
    public abstract String getRole();   // polymorphic: each subclass answers differently

    @Override public void validate() {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name is required.");
        if (email == null || !email.contains("@")) throw new IllegalArgumentException("Enter a valid email address.");
    }
}
