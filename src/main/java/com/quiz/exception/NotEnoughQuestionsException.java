package com.quiz.exception;

/**
 * Thrown when a subject does not have enough questions to build a quiz.
 */
public class NotEnoughQuestionsException extends Exception {

    private static final long serialVersionUID = 1L;

    public NotEnoughQuestionsException(String message) {
        super(message);
    }

    public NotEnoughQuestionsException(String message, Throwable cause) {
        super(message, cause);
    }
}
