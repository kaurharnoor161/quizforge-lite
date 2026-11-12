package com.quiz.dao;

import com.quiz.model.Admin;
import com.quiz.model.Student;
import com.quiz.model.User;
import com.quiz.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** JDBC (SQLite) implementation of UserDAO. */
public class JdbcUserDAO implements UserDAO {

    // Column list reused by every SELECT so mapRow always finds the same columns.
    private static final String COLUMNS = "user_id, username, password, salt, full_name, role";

    @Override
    public int insert(User user) throws SQLException {
        Objects.requireNonNull(user, "user must not be null");
        String sql = "INSERT INTO users (username, password, salt, full_name, role) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getSalt());
            ps.setString(4, user.getFullName());
            ps.setString(5, user.getRole());   // "ADMIN" or "STUDENT"
            ps.executeUpdate();

            // Ask the database which id it generated for the new row.
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int newId = keys.getInt(1);
                    user.setUserId(newId);      // also store it on the object
                    return newId;
                }
            }
        }
        throw new SQLException("Insert failed: no generated id returned for user");
    }

    @Override
    public boolean update(User user) throws SQLException {
        Objects.requireNonNull(user, "user must not be null");
        String sql = "UPDATE users SET username = ?, password = ?, salt = ?, full_name = ?, role = ? "
                   + "WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getSalt());
            ps.setString(4, user.getFullName());
            ps.setString(5, user.getRole());
            ps.setInt(6, user.getUserId());     // the id is only used in WHERE, never changed
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(int id) throws SQLException {
        // Note: foreign keys are ON, so a user who still has results cannot be deleted.
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public Optional<User> findById(int id) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM users WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<User> findAll() throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM users ORDER BY user_id";
        List<User> users = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(mapRow(rs));
            }
        }
        return users;
    }

    @Override
    public Optional<User> findByUsername(String username) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM users WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean existsByUsername(String username) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();   // true if at least one row was found
            }
        }
    }

    /** Turns the current row into an Admin or a Student, depending on the role column. */
    private User mapRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("user_id");
        String username = rs.getString("username");
        String hash = rs.getString("password");
        String salt = rs.getString("salt");
        String fullName = rs.getString("full_name");
        String role = rs.getString("role");

        if ("ADMIN".equals(role)) {
            return new Admin(id, username, hash, salt, fullName);
        }
        return new Student(id, username, hash, salt, fullName);
    }
}