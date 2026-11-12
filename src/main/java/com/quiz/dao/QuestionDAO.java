package com.quiz.dao;

import com.quiz.model.Difficulty;
import com.quiz.model.Question;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

/** DAO for questions. */
public interface QuestionDAO extends CrudDAO<Question> {

    /** Null/blank subject or null difficulty means "do not filter on that field". */
    List<Question> findByFilter(String subject, Difficulty difficulty) throws SQLException;

    /** Returns up to 'count' random questions of one subject. */
    List<Question> findRandomBySubject(String subject, int count) throws SQLException;

    int countBySubject(String subject) throws SQLException;

    /** All different subjects, stored in a HashSet. */
    Set<String> getDistinctSubjects() throws SQLException;

    /** Deletes all questions and inserts the given list in ONE transaction. */
    void replaceAll(List<Question> questions) throws SQLException;
}