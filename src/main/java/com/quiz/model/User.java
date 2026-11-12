package com.quiz.model;

/**
 * Base class for every person who can log in to the quiz application.
 * Contains no JavaFX types so it can be used from any layer.
 */
public abstract class User {

    private int userId;
    private String username;
    private String passwordHash;
    private String salt;
    private String fullName;

    /** Constructor for rows loaded from the database (id already known). */
    public User(int userId, String username, String passwordHash, String salt, String fullName) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.fullName = fullName;
    }

    /** Constructor for new users that have not been saved yet (id is 0 until the DB assigns one). */
    public User(String username, String passwordHash, String salt, String fullName) {
        this(0, username, passwordHash, salt, fullName);
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    /** Returns the role name, e.g. "ADMIN" or "STUDENT". */
    public abstract String getRole();

    /** Returns the title shown at the top of this user's dashboard. */
    public abstract String getDashboardTitle();

    /** The password hash and salt are deliberately left out of the text. */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "{userId=" + userId
                + ", username='" + username + '\''
                + ", fullName='" + fullName + '\''
                + ", role='" + getRole() + "'}";
    }
}
