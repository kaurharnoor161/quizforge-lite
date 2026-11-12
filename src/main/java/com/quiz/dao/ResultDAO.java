package com.quiz.dao;

import com.quiz.model.Result;

import java.sql.SQLException;
import java.util.List;

/** DAO for quiz results. */
public interface ResultDAO extends CrudDAO<Result> {

    /** All attempts of one user, newest first. */
    List<Result> findByUserId(int userId) throws SQLException;

    /** All attempts of all users; studentName is filled from users.full_name. */
    List<Result> findAllWithStudentNames() throws SQLException;
}