package com.quiz.util;

import com.quiz.model.Result;

import java.util.Comparator;

/**
 * Sorting rule for results (no JavaFX):
 * higher score first; if equal, higher percentage first; if still equal, newer attempt first.
 */
public class ResultScoreComparator implements Comparator<Result> {

    @Override
    public int compare(Result a, Result b) {
        // Swapping a and b gives DESCENDING order (biggest first)
        int byScore = Integer.compare(b.getScore(), a.getScore());
        if (byScore != 0) {
            return byScore;
        }

        int byPercentage = Integer.compare(b.getPercentage(), a.getPercentage());
        if (byPercentage != 0) {
            return byPercentage;
        }

        // newer date first
        return b.getAttemptedOn().compareTo(a.getAttemptedOn());
    }
}