package com.quiz.model;

/**
 * A student who takes quizzes and views their own results.
 */
public class Student extends User {

    /** For rows loaded from the database. */
    public Student(int userId, String username, String passwordHash, String salt, String fullName) {
        super(userId, username, passwordHash, salt, fullName);
    }

    /** For new students not yet saved. */
    public Student(String username, String passwordHash, String salt, String fullName) {
        super(username, passwordHash, salt, fullName);
    }

    @Override
    public String getRole() {
        return "STUDENT";
    }

    @Override
    public String getDashboardTitle() {
        return "Student Dashboard - Take a Quiz";
    }
}
