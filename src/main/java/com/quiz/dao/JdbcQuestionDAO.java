package com.quiz.dao;

import com.quiz.model.Difficulty;
import com.quiz.model.Question;
import com.quiz.util.AppLogger;
import com.quiz.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** JDBC (SQLite) implementation of QuestionDAO. */
public class JdbcQuestionDAO implements QuestionDAO {

    private static final String COLUMNS =
            "question_id, subject, question_text, option_a, option_b, option_c, option_d, "
          + "correct_option, difficulty";

    private static final String INSERT_SQL =
            "INSERT INTO questions (subject, question_text, option_a, option_b, option_c, option_d, "
          + "correct_option, difficulty) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    @Override
    public int insert(Question question) throws SQLException {
        Objects.requireNonNull(question, "question must not be null");
        try (Connection conn = DBConnection.getConnection()) {
            return insertWith(conn, question);
        }
    }

    /**
     * Inserts one question using a connection that is given to us.
     * insert() and replaceAll() both use this, so the SQL exists only once.
     */
    private int insertWith(Connection conn, Question question) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, question);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int newId = keys.getInt(1);
                    question.setQuestionId(newId);   // store the new id on the object
                    return newId;
                }
            }
        }
        throw new SQLException("Insert failed: no generated id returned for question");
    }

    @Override
    public boolean update(Question question) throws SQLException {
        Objects.requireNonNull(question, "question must not be null");
        String sql = "UPDATE questions SET subject = ?, question_text = ?, option_a = ?, option_b = ?, "
                   + "option_c = ?, option_d = ?, correct_option = ?, difficulty = ? "
                   + "WHERE question_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, question);                       // fills placeholders 1 to 8
            ps.setInt(9, question.getQuestionId());   // placeholder 9 is the WHERE id
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM questions WHERE question_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public Optional<Question> findById(int id) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM questions WHERE question_id = ?";
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
    public List<Question> findAll() throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM questions ORDER BY question_id";
        List<Question> list = new ArrayList<>();
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
    public List<Question> findByFilter(String subject, Difficulty difficulty) throws SQLException {
        boolean hasSubject = subject != null && !subject.isBlank();
        boolean hasDifficulty = difficulty != null;

        // Only fixed text like "subject = ?" is appended here. User values are NEVER
        // joined into the SQL; they are bound later with setString().
        StringBuilder sql = new StringBuilder("SELECT " + COLUMNS + " FROM questions");
        if (hasSubject || hasDifficulty) {
            sql.append(" WHERE ");
            if (hasSubject) {
                sql.append("subject = ?");
            }
            if (hasSubject && hasDifficulty) {
                sql.append(" AND ");
            }
            if (hasDifficulty) {
                sql.append("difficulty = ?");
            }
        }
        sql.append(" ORDER BY question_id");

        List<Question> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int index = 1;   // placeholders are numbered in the order they appear
            if (hasSubject) {
                ps.setString(index++, subject.trim());
            }
            if (hasDifficulty) {
                ps.setString(index++, difficulty.name());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    @Override
    public List<Question> findRandomBySubject(String subject, int count) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM questions WHERE subject = ? ORDER BY RANDOM() LIMIT ?";
        List<Question> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, subject);
            ps.setInt(2, count);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    @Override
    public int countBySubject(String subject) throws SQLException {
        String sql = "SELECT COUNT(*) FROM questions WHERE subject = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, subject);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    @Override
    public Set<String> getDistinctSubjects() throws SQLException {
        String sql = "SELECT DISTINCT subject FROM questions";
        Set<String> subjects = new HashSet<>();   // a HashSet never keeps duplicates
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                subjects.add(rs.getString("subject"));
            }
        }
        return subjects;
    }

    /**
     * Replaces the whole question bank in ONE transaction.
     * Either everything succeeds (old rows gone, new rows saved) or nothing changes.
     */
    @Override
    public void replaceAll(List<Question> questions) throws SQLException {
        Objects.requireNonNull(questions, "questions must not be null");

        // We do NOT use try-with-resources for the connection here, because the finally
        // block must first switch autoCommit back on and only then close it.
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();

            // STEP 1: turn autoCommit OFF. Now nothing is saved permanently until we call
            // commit(). All the statements below belong to one single transaction.
            conn.setAutoCommit(false);

            // STEP 2a: delete every existing question (not saved yet, only inside the transaction).
            try (PreparedStatement delete = conn.prepareStatement("DELETE FROM questions")) {
                delete.executeUpdate();
            }

            // STEP 2b: insert every question from the list using the SAME connection.
            // If one question breaks a rule (for example a NOT NULL column is null),
            // the database throws an SQLException and we jump to the catch block.
            for (Question q : questions) {
                insertWith(conn, q);
            }

            // STEP 3: everything worked, so make all the changes permanent in one go.
            conn.commit();
            logInfo("replaceAll committed: " + questions.size() + " questions saved");

        } catch (SQLException | RuntimeException e) {
            // STEP 4: something failed, so undo EVERYTHING done since setAutoCommit(false).
            // The old questions come back as if nothing had happened.
            if (conn != null) {
                try {
                    conn.rollback();
                    logError("replaceAll rolled back, old questions kept", e);
                } catch (SQLException rollbackError) {
                    logError("rollback itself failed", rollbackError);
                }
            }
            throw e;   // rethrow so the caller (later: the Restore button) knows it failed

        } finally {
            // STEP 5: always runs, success or failure. Put the connection back to its
            // normal mode (autoCommit on) and close it so no resource is leaked.
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    logError("could not restore autoCommit", e);
                }
                try {
                    conn.close();
                } catch (SQLException e) {
                    logError("could not close connection", e);
                }
            }
        }
    }

    /** Sets the 8 question placeholders (subject ... difficulty) on a statement. */
    private void bind(PreparedStatement ps, Question q) throws SQLException {
        ps.setString(1, q.getSubject());
        ps.setString(2, q.getQuestionText());
        ps.setString(3, q.getOptionA());
        ps.setString(4, q.getOptionB());
        ps.setString(5, q.getOptionC());
        ps.setString(6, q.getOptionD());
        ps.setString(7, String.valueOf(q.getCorrectOption()));
        // A null difficulty becomes a NULL in SQL, which the NOT NULL rule then rejects.
        ps.setString(8, q.getDifficulty() == null ? null : q.getDifficulty().name());
    }

    /** Turns the current row into a Question (uses the constructor that has an id). */
    private Question mapRow(ResultSet rs) throws SQLException {
        return new Question(
                rs.getInt("question_id"),
                rs.getString("subject"),
                rs.getString("question_text"),
                rs.getString("option_a"),
                rs.getString("option_b"),
                rs.getString("option_c"),
                rs.getString("option_d"),
                rs.getString("correct_option").charAt(0),
                Difficulty.fromString(rs.getString("difficulty")));
    }

    // ---- logging helpers: if AppLogger has different method names, change ONLY these two ----
    private static void logInfo(String message) {
        AppLogger.getLogger().info(message);
    }

    private static void logError(String message, Exception e) {
        AppLogger.getLogger().severe(message + " - " + e.getMessage());
    }
}