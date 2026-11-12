package com.quiz.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Creates database connections and builds the tables on first run.
 * Every caller is responsible for closing the Connection (use try-with-resources).
 */
public final class DBConnection {

    private DBConnection() {
    }

    // IF NOT EXISTS means: only create the table when it is missing
    private static final String CREATE_USERS =
            "CREATE TABLE IF NOT EXISTS users ("
            + " user_id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + " username TEXT UNIQUE NOT NULL,"
            + " password TEXT NOT NULL,"
            + " salt TEXT NOT NULL,"
            + " full_name TEXT NOT NULL,"
            + " role TEXT CHECK(role IN ('ADMIN','STUDENT')) NOT NULL"
            + ")";

    private static final String CREATE_QUESTIONS =
            "CREATE TABLE IF NOT EXISTS questions ("
            + " question_id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + " subject TEXT NOT NULL,"
            + " question_text TEXT NOT NULL,"
            + " option_a TEXT NOT NULL, option_b TEXT NOT NULL,"
            + " option_c TEXT NOT NULL, option_d TEXT NOT NULL,"
            + " correct_option CHAR(1) CHECK(correct_option IN ('A','B','C','D')) NOT NULL,"
            + " difficulty TEXT CHECK(difficulty IN ('EASY','MEDIUM','HARD')) NOT NULL"
            + ")";

    private static final String CREATE_RESULTS =
            "CREATE TABLE IF NOT EXISTS results ("
            + " result_id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + " user_id INTEGER NOT NULL REFERENCES users(user_id),"
            + " subject TEXT NOT NULL,"
            + " score INTEGER NOT NULL,"
            + " total INTEGER NOT NULL,"
            + " attempted_on TEXT NOT NULL"
            + ")";

    /** Opens a new connection to the SQLite file named in config.properties. */
    public static Connection getConnection() throws SQLException {
        String url = "jdbc:sqlite:" + ConfigLoader.getString("db.path");
        Connection conn = DriverManager.getConnection(url);
        // SQLite ignores foreign keys unless we switch them on for each connection
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
    }

    /** Creates the 3 tables if they do not exist yet. */
    public static void initSchema() throws SQLException {
        // try-with-resources closes the Statement first, then the Connection
        try (Connection conn = getConnection();
             Statement st = conn.createStatement()) {
            st.executeUpdate(CREATE_USERS);
            st.executeUpdate(CREATE_QUESTIONS);
            st.executeUpdate(CREATE_RESULTS);
            AppLogger.info("Database schema checked/created");
        }
    }

    /** Counts our tables (used by Main to prove the setup worked). */
    public static int countTables() throws SQLException {
        // sqlite_master lists every table; internal tables start with "sqlite_" so we skip them
        String sql = "SELECT COUNT(*) FROM sqlite_master "
                   + "WHERE type = 'table' AND name NOT LIKE 'sqlite_%'";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}