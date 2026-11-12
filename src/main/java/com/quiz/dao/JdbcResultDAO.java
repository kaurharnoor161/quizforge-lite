package com.quiz.dao;

import com.quiz.model.Result;
import com.quiz.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** JDBC (SQLite) implementation of ResultDAO. */
public class JdbcResultDAO implements ResultDAO {

    private static final String COLUMNS = "result_id, user_id, subject, score, total, attempted_on";

    @Override
    public int insert(Result result) throws SQLException {
        Objects.requireNonNull(result, "result must not be null");
        String sql = "INSERT INTO results (user_id, subject, score, total, attempted_on) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, result.getUserId());
            ps.setString(2, result.getSubject());
            ps.setInt(3, result.getScore());
            ps.setInt(4, result.getTotal());
            ps.setString(5, result.getAttemptedOn().toString());   // stored as ISO text
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int newId = keys.getInt(1);
                    result.setResultId(newId);
                    return newId;
                }
            }
        }
        throw new SQLException("Insert failed: no generated id returned for result");
    }

    @Override
    public boolean update(Result result) throws SQLException {
        Objects.requireNonNull(result, "result must not be null");
        String sql = "UPDATE results SET user_id = ?, subject = ?, score = ?, total = ?, attempted_on = ? "
                   + "WHERE result_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, result.getUserId());
            ps.setString(2, result.getSubject());
            ps.setInt(3, result.getScore());
            ps.setInt(4, result.getTotal());
            ps.setString(5, result.getAttemptedOn().toString());
            ps.setInt(6, result.getResultId());
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM results WHERE result_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public Optional<Result> findById(int id) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM results WHERE result_id = ?";
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
    public List<Result> findAll() throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM results ORDER BY result_id";
        List<Result> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    @Override
    public List<Result> findByUserId(int userId) throws SQLException {
        // ISO text sorts correctly as plain text, so DESC gives newest first.
        String sql = "SELECT " + COLUMNS + " FROM results WHERE user_id = ? "
                   + "ORDER BY attempted_on DESC, result_id DESC";
        List<Result> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    @Override
    public List<Result> findAllWithStudentNames() throws SQLException {
        // JOIN links each result to its user so we can read the student's full name.
        String sql = "SELECT r.result_id, r.user_id, r.subject, r.score, r.total, r.attempted_on, u.full_name "
                   + "FROM results r JOIN users u ON r.user_id = u.user_id "
                   + "ORDER BY r.attempted_on DESC, r.result_id DESC";
        List<Result> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Result result = mapRow(rs);
                result.setStudentName(rs.getString("full_name"));
                list.add(result);
            }
        }
        return list;
    }

    /** Turns the current row into a Result (text date is parsed back to LocalDateTime). */
    private Result mapRow(ResultSet rs) throws SQLException {
        return new Result(
                rs.getInt("result_id"),
                rs.getInt("user_id"),
                rs.getString("subject"),
                rs.getInt("score"),
                rs.getInt("total"),
                LocalDateTime.parse(rs.getString("attempted_on")));
    }
}