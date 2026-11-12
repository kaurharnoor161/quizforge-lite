package com.quiz.dao;

import com.quiz.model.User;

import java.sql.SQLException;
import java.util.Optional;

/** DAO for users. Adds username lookups on top of the basic CRUD methods. */
public interface UserDAO extends CrudDAO<User> {

    Optional<User> findByUsername(String username) throws SQLException;

    boolean existsByUsername(String username) throws SQLException;
}
