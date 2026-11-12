package com.quiz.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/**
 * One quiz attempt by one student.
 */
public class Result {

    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH);

    private int resultId;
    private int userId;
    private String subject;
    private int score;
    private int total;
    private LocalDateTime attemptedOn;
    private String studentName = "";

    /** Constructor for rows loaded from the database (id already known). */
    public Result(int resultId, int userId, String subject, int score, int total,
                  LocalDateTime attemptedOn) {
        this.resultId = resultId;
        this.userId = userId;
        this.subject = Objects.requireNonNull(subject, "subject must not be null");
        this.score = score;
        this.total = total;
        this.attemptedOn = Objects.requireNonNull(attemptedOn, "attemptedOn must not be null");
    }

    /** Constructor for new results not yet saved (id is 0 until the DB assigns one). */
    public Result(int userId, String subject, int score, int total, LocalDateTime attemptedOn) {
        this(0, userId, subject, score, total, attemptedOn);
    }

    public int getResultId() {
        return resultId;
    }

    public void setResultId(int resultId) {
        this.resultId = resultId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = Objects.requireNonNull(subject, "subject must not be null");
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public LocalDateTime getAttemptedOn() {
        return attemptedOn;
    }

    public void setAttemptedOn(LocalDateTime attemptedOn) {
        this.attemptedOn = Objects.requireNonNull(attemptedOn, "attemptedOn must not be null");
    }

    /** Student's display name, filled in later for the admin results table. Never null. */
    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = (studentName == null) ? "" : studentName;
    }

    /** Score as a rounded percentage; 0 when total is 0. */
    public int getPercentage() {
        if (total == 0) {
            return 0;
        }
        return (int) Math.round(score * 100.0 / total);
    }

    /** Readable date-time, e.g. "09 Nov 2026, 14:30". */
    public String getAttemptedOnText() {
        return attemptedOn.format(DISPLAY_FORMAT);
    }

    @Override
    public String toString() {
        return "Result{resultId=" + resultId
                + ", userId=" + userId
                + ", studentName='" + studentName + '\''
                + ", subject='" + subject + '\''
                + ", score=" + score
                + ", total=" + total
                + ", percentage=" + getPercentage()
                + ", attemptedOn=" + getAttemptedOnText() + '}';
    }
}
