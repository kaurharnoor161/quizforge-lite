package com.quiz.util;

import com.quiz.model.Difficulty;
import com.quiz.model.Question;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Checks form values and builds a Question. No JavaFX in here,
 * so it can be tested from the console.
 */
public final class QuestionValidator {

    private static final int MAX_SUBJECT_LENGTH = 30;
    private static final int MAX_QUESTION_LENGTH = 500;
    private static final int MAX_OPTION_LENGTH = 200;

    private static final String[] OPTION_NAMES = {"A", "B", "C", "D"};

    private QuestionValidator() {
        // utility class - nobody should create an object of it
    }

    /**
     * Trims all text, validates it and returns a new Question (id stays 0).
     * Null or blank text from a form is a validation error, not a crash,
     * so Objects.requireNonNull is deliberately not used here.
     *
     * @throws IllegalArgumentException with a specific message if something is wrong
     */
    public static Question createQuestion(String subject, String questionText,
                                          String optionA, String optionB,
                                          String optionC, String optionD,
                                          String correctOption, Difficulty difficulty) {

        String cleanSubject = trim(subject);
        String cleanText = trim(questionText);
        String[] options = {trim(optionA), trim(optionB), trim(optionC), trim(optionD)};
        String cleanCorrect = trim(correctOption);

        // Subject
        if (cleanSubject.isEmpty()) {
            throw fail("Subject must not be empty");
        }
        if (cleanSubject.length() > MAX_SUBJECT_LENGTH) {
            throw fail("Subject must not be longer than " + MAX_SUBJECT_LENGTH + " characters");
        }

        // Question text
        if (cleanText.isEmpty()) {
            throw fail("Question text must not be empty");
        }
        if (cleanText.length() > MAX_QUESTION_LENGTH) {
            throw fail("Question text must not be longer than " + MAX_QUESTION_LENGTH + " characters");
        }

        // Options: each one present and not too long
        for (int i = 0; i < options.length; i++) {
            if (options[i].isEmpty()) {
                throw fail("Option " + OPTION_NAMES[i] + " must not be empty");
            }
            if (options[i].length() > MAX_OPTION_LENGTH) {
                throw fail("Option " + OPTION_NAMES[i] + " must not be longer than "
                        + MAX_OPTION_LENGTH + " characters");
            }
        }

        // Options: all four different (ignoring case)
        Set<String> seen = new HashSet<>();
        for (String option : options) {
            if (!seen.add(option.toLowerCase(Locale.ROOT))) {
                throw fail("All four options must be different");
            }
        }

        // Correct option: exactly one of A, B, C, D
        if (cleanCorrect.length() != 1 || "ABCD".indexOf(cleanCorrect.charAt(0)) < 0) {
            throw fail("Correct option must be one of A, B, C or D");
        }

        // Difficulty
        if (difficulty == null) {
            throw fail("Difficulty must be selected");
        }

        // Question stores the correct option as a char; the check above
        // guarantees cleanCorrect is exactly one character from A to D.
        return new Question(cleanSubject, cleanText,
                options[0], options[1], options[2], options[3],
                cleanCorrect.charAt(0), difficulty);
    }

    /** Null becomes an empty string, everything else is trimmed. */
    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    /** Logs a validation problem and returns the exception to throw. */
    private static IllegalArgumentException fail(String message) {
        AppLogger.warning("Question validation failed: " + message);
        return new IllegalArgumentException(message);
    }
}