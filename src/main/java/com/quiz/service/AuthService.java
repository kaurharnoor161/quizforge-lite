package com.quiz.service;

import com.quiz.dao.JdbcUserDAO;
import com.quiz.dao.UserDAO;
import com.quiz.exception.DuplicateUserException;
import com.quiz.exception.InvalidLoginException;
import com.quiz.model.Student;
import com.quiz.model.User;
import com.quiz.util.AppLogger;
import com.quiz.util.PasswordUtil;

import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Business logic for logging in and registering.
 * This class has NO JavaFX code, so it can be tested from the console.
 */
public class AuthService {

    // 3 to 20 characters, only letters, digits and underscore.
    private static final Pattern USERNAME_PATTERN = Pattern.compile("[A-Za-z0-9_]{3,20}");
    private static final int MIN_PASSWORD_LENGTH = 6;

    // One message for BOTH "unknown user" and "wrong password", so an attacker
    // cannot find out which usernames exist.
    private static final String LOGIN_ERROR = "Invalid username or password";

    private final UserDAO userDao;

    /** Normal constructor: the DAO is given from outside (easy to replace in tests). */
    public AuthService(UserDAO userDao) {
        this.userDao = Objects.requireNonNull(userDao, "userDao must not be null");
    }

    /** Convenience constructor that uses the real SQLite DAO. */
    public AuthService() {
        this(new JdbcUserDAO());
    }

    /**
     * Checks username and password.
     * Returns the logged-in User (an Admin or a Student object).
     */
    public User login(String username, String password) throws InvalidLoginException {
        Objects.requireNonNull(username, "username must not be null");
        Objects.requireNonNull(password, "password must not be null");
        String name = username.trim();

        try {
            Optional<User> found = userDao.findByUsername(name);

            // If the user does not exist OR the password is wrong -> same error.
            if (found.isEmpty() || !passwordMatches(password, found.get())) {
                logWarn("Login failed for username: " + name);   // never log the password
                throw new InvalidLoginException(LOGIN_ERROR);
            }

            logInfo("Login successful: " + name + " (" + found.get().getRole() + ")");
            return found.get();

        } catch (SQLException e) {
            logError("Database error during login", e);
            throw new IllegalStateException("Database error", e);
        }
    }

    /**
     * Creates a new STUDENT account. Nobody can register as admin from here.
     * Returns the saved Student (with its generated id).
     */
    public Student register(String username, String password, String fullName)
            throws DuplicateUserException {
        Objects.requireNonNull(username, "username must not be null");
        Objects.requireNonNull(password, "password must not be null");
        Objects.requireNonNull(fullName, "fullName must not be null");

        String name = username.trim();
        String display = fullName.trim();

        // ---- validation (these are unchecked exceptions, thrown before touching the database)
        if (!USERNAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException(
                    "Username must be 3 to 20 characters: letters, digits and underscore only");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        if (display.isEmpty()) {
            throw new IllegalArgumentException("Full name must not be blank");
        }

        try {
            if (userDao.existsByUsername(name)) {
                logWarn("Registration refused, username already taken: " + name);
                throw new DuplicateUserException("Username already taken");
            }

            // Fresh random salt for every user, then hash "password + salt".
            String salt = PasswordUtil.generateSalt();
            String hash = PasswordUtil.hashPassword(password, salt);

            // The role is ALWAYS student: the Student class sets it itself.
            Student student = new Student(name, hash, salt, display);
            userDao.insert(student);   // insert() also sets the generated id on the object

            logInfo("New student registered: " + name);
            return student;

        } catch (SQLException e) {
            logError("Database error during registration", e);
            throw new IllegalStateException("Database error", e);
        }
    }

    /** Re-hashes the typed password with the stored salt and compares with the stored hash. */
    private boolean passwordMatches(String typedPassword, User user) {
        return PasswordUtil.verifyPassword(typedPassword, user.getSalt(), user.getPasswordHash());
    }

    // ---- small logging helpers that call your real AppLogger ----
    private static void logInfo(String message) {
        AppLogger.info(message);
    }

    // AppLogger only has info() and severe() (as far as I have seen), so warnings go through info().
    // If your AppLogger has warning(String), change this one line to AppLogger.warning(message).
    private static void logWarn(String message) {
        AppLogger.warning(message);
    }

    private static void logError(String message, Exception e) {
        AppLogger.severe(message, e);
    }
}
