package com.campus.model;

/** A person who can sign in. The password hash is never part of this object. */
public class AppUser {
    private final int id;
    private final String username;
    private final String fullName;
    private final String role;   // ADMIN or STUDENT

    public AppUser(int id, String username, String fullName, String role) {
        this.id = id; this.username = username; this.fullName = fullName; this.role = role;
    }
    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getRole() { return role; }
    public boolean isAdmin() { return "ADMIN".equals(role); }
}
