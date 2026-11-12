package com.quiz.exception;

/**
 * Thrown when registering a username that already exists.
 */
public class DuplicateUserException extends Exception {

    private static final long serialVersionUID = 1L;

    public DuplicateUserException(String message) {
        super(message);
    }

    public DuplicateUserException(String message, Throwable cause) {
        super(message, cause);
    }
}
