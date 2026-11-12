package com.quiz.model;

/**
 * An administrator who manages questions and views all results.
 */
public class Admin extends User {

    /** For rows loaded from the database. */
    public Admin(int userId, String username, String passwordHash, String salt, String fullName) {
        super(userId, username, passwordHash, salt, fullName);
    }

    /** For new admins not yet saved. */
    public Admin(String username, String passwordHash, String salt, String fullName) {
        super(username, passwordHash, salt, fullName);
    }

    @Override
    public String getRole() {
        return "ADMIN";
    }

    @Override
    public String getDashboardTitle() {
        return "Admin Dashboard - Manage Questions & Results";
    }
}
