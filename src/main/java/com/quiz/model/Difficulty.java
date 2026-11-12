package com.quiz.model;

import java.util.Locale;

/**
 * Difficulty levels for a quiz question.
 */
public enum Difficulty {
    EASY,
    MEDIUM,
    HARD;

    /**
     * Converts text such as "easy", "Medium" or " HARD " into a Difficulty.
     *
     * @throws IllegalArgumentException if s is null, blank or not a valid difficulty
     */
    public static Difficulty fromString(String s) {
        if (s == null || s.trim().isEmpty()) {
            throw new IllegalArgumentException("Difficulty must not be null or blank");
        }
        try {
            return Difficulty.valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid difficulty: '" + s + "' (expected EASY, MEDIUM or HARD)", e);
        }
    }
}
