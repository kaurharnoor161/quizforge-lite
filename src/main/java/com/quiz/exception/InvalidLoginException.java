package com.quiz.exception;

/**
 * Thrown when a username or password is wrong.
 */
public class InvalidLoginException extends Exception {

    private static final long serialVersionUID = 1L;

    public InvalidLoginException(String message) {
        super(message);
    }

    public InvalidLoginException(String message, Throwable cause) {
        super(message, cause);
    }
}
