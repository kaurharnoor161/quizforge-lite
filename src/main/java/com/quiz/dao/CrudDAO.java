package com.quiz.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Generic CRUD interface. T is the type of object (User, Question, Result).
 * Every method declares SQLException so database errors are never hidden.
 */
public interface CrudDAO<T> {

    /** Inserts the item, sets the new id on it and returns that id. */
    int insert(T item) throws SQLException;

    /** Updates the row with the item's id. Returns true if a row was changed. */
    boolean update(T item) throws SQLException;

    /** Deletes the row with this id. Returns true if a row was deleted. */
    boolean delete(int id) throws SQLException;

    /** Finds one row by id. Optional.empty() means "not found". */
    Optional<T> findById(int id) throws SQLException;

    /** Returns every row as a list. */
    List<T> findAll() throws SQLException;
}