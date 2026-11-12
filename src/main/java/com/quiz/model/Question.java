package com.quiz.model;

import java.io.Serializable;

/**
 * A multiple-choice question with four options (A-D).
 * Serializable so the question bank can be backed up and restored.
 */
public class Question implements Serializable {

    private static final long serialVersionUID = 1L;

    private int questionId;
    private String subject;
    private String questionText;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private char correctOption;
    private Difficulty difficulty;

    /** Constructor for rows loaded from the database (id already known). */
    public Question(int questionId, String subject, String questionText,
                    String optionA, String optionB, String optionC, String optionD,
                    char correctOption, Difficulty difficulty) {
        this.questionId = questionId;
        this.subject = subject;
        this.questionText = questionText;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
        this.correctOption = correctOption;
        this.difficulty = difficulty;
    }

    /** Constructor for new questions not yet saved (id is 0 until the DB assigns one). */
    public Question(String subject, String questionText,
                    String optionA, String optionB, String optionC, String optionD,
                    char correctOption, Difficulty difficulty) {
        this(0, subject, questionText, optionA, optionB, optionC, optionD, correctOption, difficulty);
    }

    public int getQuestionId() {
        return questionId;
    }

    public void setQuestionId(int questionId) {
        this.questionId = questionId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getOptionA() {
        return optionA;
    }

    public void setOptionA(String optionA) {
        this.optionA = optionA;
    }

    public String getOptionB() {
        return optionB;
    }

    public void setOptionB(String optionB) {
        this.optionB = optionB;
    }

    public String getOptionC() {
        return optionC;
    }

    public void setOptionC(String optionC) {
        this.optionC = optionC;
    }

    public String getOptionD() {
        return optionD;
    }

    public void setOptionD(String optionD) {
        this.optionD = optionD;
    }

    public char getCorrectOption() {
        return correctOption;
    }

    public void setCorrectOption(char correctOption) {
        this.correctOption = correctOption;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    /**
     * Returns the text of option A, B, C or D (case-insensitive).
     *
     * @throws IllegalArgumentException for any other character
     */
    public String getOptionText(char option) {
        switch (Character.toUpperCase(option)) {
            case 'A':
                return optionA;
            case 'B':
                return optionB;
            case 'C':
                return optionC;
            case 'D':
                return optionD;
            default:
                throw new IllegalArgumentException(
                        "Option must be A, B, C or D but was: '" + option + "'");
        }
    }

    /**
     * A question is valid when subject, question text and all four options are
     * non-blank, the correct option is exactly A, B, C or D, and difficulty is set.
     */
    public boolean isValid() {
        if (isBlank(subject) || isBlank(questionText)
                || isBlank(optionA) || isBlank(optionB)
                || isBlank(optionC) || isBlank(optionD)) {
            return false;
        }
        if (correctOption != 'A' && correctOption != 'B'
                && correctOption != 'C' && correctOption != 'D') {
            return false;
        }
        return difficulty != null;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    @Override
    public String toString() {
        return "Question{questionId=" + questionId
                + ", subject='" + subject + '\''
                + ", questionText='" + questionText + '\''
                + ", optionA='" + optionA + '\''
                + ", optionB='" + optionB + '\''
                + ", optionC='" + optionC + '\''
                + ", optionD='" + optionD + '\''
                + ", correctOption=" + correctOption
                + ", difficulty=" + difficulty + '}';
    }
}
