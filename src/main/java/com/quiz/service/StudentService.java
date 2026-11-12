package com.quiz.service;

import com.quiz.dao.JdbcQuestionDAO;
import com.quiz.dao.JdbcResultDAO;
import com.quiz.dao.QuestionDAO;
import com.quiz.dao.ResultDAO;
import com.quiz.model.Result;
import com.quiz.util.AppLogger;
import com.quiz.util.ConfigLoader;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Logic for the student home screen (no JavaFX here). */
public class StudentService {

    private final QuestionDAO questionDao;
    private final ResultDAO resultDao;

    public StudentService(QuestionDAO questionDao, ResultDAO resultDao) {
        this.questionDao = Objects.requireNonNull(questionDao, "questionDao must not be null");
        this.resultDao = Objects.requireNonNull(resultDao, "resultDao must not be null");
    }

    public StudentService() {
        this(new JdbcQuestionDAO(), new JdbcResultDAO());
    }

    /** All subjects, sorted A-Z. */
    public List<String> getSubjects() {
        try {
            Set<String> distinct = questionDao.getDistinctSubjects(); // a HashSet (no order)
            List<String> subjects = new ArrayList<>(distinct);        // copy into a list...
            Collections.sort(subjects);                               // ...so we can sort it
            return subjects;
        } catch (SQLException e) {
            throw databaseError("Could not load the subjects", e);
        }
    }

    /** How many questions exist for a subject. */
    public int getQuestionCount(String subject) {
        try {
            return questionDao.countBySubject(subject);
        } catch (SQLException e) {
            throw databaseError("Could not count the questions of subject " + subject, e);
        }
    }

    /** Number of questions in one quiz (from config.properties). */
    public int getQuizSize() {
        return ConfigLoader.getInt("questions.per.quiz");
    }

    /** All attempts of one student, newest first (the DAO sorts them). */
    public List<Result> getHistory(int userId) {
        try {
            return resultDao.findByUserId(userId);
        } catch (SQLException e) {
            throw databaseError("Could not load the attempts of user " + userId, e);
        }
    }

    /** The last 'max' attempts of one student, newest first. */
    public LinkedList<Result> getRecentAttempts(int userId, int max) {
        return buildRecent(getHistory(userId), max);
    }

    /**
     * Keeps only the newest 'max' results.
     *
     * Why LinkedList: we add at the front (addFirst) and drop from the end (removeLast).
     * Both are quick, simple operations at the two ends of a LinkedList.
     *
     * We walk from the OLDEST result to the NEWEST (backwards through the list), so every new
     * result is pushed to the front. At the end the newest result is first.
     * The input list is only read, never changed.
     */
    public static LinkedList<Result> buildRecent(List<Result> newestFirst, int max) {
        Objects.requireNonNull(newestFirst, "newestFirst must not be null");
        if (max < 1) {
            throw new IllegalArgumentException("max must be at least 1");
        }

        LinkedList<Result> recent = new LinkedList<>();
        for (int i = newestFirst.size() - 1; i >= 0; i--) {
            recent.addFirst(newestFirst.get(i));
            if (recent.size() > max) {
                recent.removeLast(); // the oldest one falls out
            }
        }
        return recent;
    }

    /** Logs the SQLException and wraps it, the same way AuthService does it. */
    private IllegalStateException databaseError(String message, SQLException e) {
        AppLogger.severe(message, e);
        return new IllegalStateException("Database error", e);
    }
}