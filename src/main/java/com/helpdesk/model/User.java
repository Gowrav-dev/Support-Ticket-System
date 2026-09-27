package com.helpdesk.model;

/**
 * Model class representing a User or Support Staff employee.
 */
public class User {
    private int id;
    private String username;
    private transient String passwordHash; // transient so it is never sent in JSON
    private String fullName;
    private String role; // "USER" or "SUPPORT_STAFF"

    public User() {}

    public User(int id, String username, String passwordHash, String fullName, String role) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
