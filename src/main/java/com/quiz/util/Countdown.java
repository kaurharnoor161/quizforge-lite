package com.quiz.util;

/**
 * A simple countdown (no JavaFX here). It only keeps the numbers;
 * the quiz window calls tick() once per second.
 */
public final class Countdown {

    private final int total;   // never changes
    private int remaining;     // goes down by 1 on every tick

    public Countdown(int totalSeconds) {
        if (totalSeconds < 1) {
            throw new IllegalArgumentException("totalSeconds must be at least 1");
        }
        this.total = totalSeconds;
        this.remaining = totalSeconds;
    }

    public int getTotal() {
        return total;
    }

    public int getRemaining() {
        return remaining;
    }

    /** Lowers the remaining time by 1 (never below 0) and returns the new value. */
    public int tick() {
        if (remaining > 0) {
            remaining--;
        }
        return remaining;
    }

    public boolean isFinished() {
        return remaining == 0;
    }

    /** True when 30 seconds or less are left (this includes 0). */
    public boolean isWarning() {
        return remaining <= 30;
    }

    /** "MM:SS" with two digits each. Minutes may be above 59 (3600 seconds gives "60:00"). */
    public String getDisplayText() {
        int minutes = remaining / 60;
        int seconds = remaining % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}