package com.quiz.util;

import com.quiz.dao.JdbcQuestionDAO;
import com.quiz.dao.JdbcUserDAO;
import com.quiz.dao.QuestionDAO;
import com.quiz.dao.UserDAO;
import com.quiz.model.Admin;
import com.quiz.model.Difficulty;
import com.quiz.model.Question;
import com.quiz.model.Student;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Puts demo data into an empty database: 2 users and 30 questions.
 * Running it again does nothing, so there are never duplicates.
 */
public final class DataSeeder {

    private DataSeeder() {
        // utility class: nobody should create an object of it
    }

    public static void seedIfEmpty() throws SQLException {
        seedUsers();
        seedQuestions();
    }

    // ------------------------------------------------------------ users

    private static void seedUsers() throws SQLException {
        UserDAO userDao = new JdbcUserDAO();
        if (!userDao.findAll().isEmpty()) {
            logInfo("Seeder: users table already has data, skipped");
            return;
        }

        // Every user gets their own random salt, then the password is hashed with it.
        String adminSalt = PasswordUtil.generateSalt();
        String adminHash = PasswordUtil.hashPassword("admin123", adminSalt);
        userDao.insert(new Admin("admin", adminHash, adminSalt, "Administrator"));

        String studentSalt = PasswordUtil.generateSalt();
        String studentHash = PasswordUtil.hashPassword("student123", studentSalt);
        userDao.insert(new Student("student1", studentHash, studentSalt, "Demo Student"));

        logInfo("Seeder: inserted 2 users (admin, student1)");
    }

    // -------------------------------------------------------- questions

    private static void seedQuestions() throws SQLException {
        QuestionDAO questionDao = new JdbcQuestionDAO();
        if (!questionDao.findAll().isEmpty()) {
            logInfo("Seeder: questions table already has data, skipped");
            return;
        }

        List<Question> list = new ArrayList<>();
        final String J = "Java";
        final String S = "SQL";
        final Difficulty easy = Difficulty.EASY;
        final Difficulty med = Difficulty.MEDIUM;
        final Difficulty hard = Difficulty.HARD;

        // ---------- Java: EASY ----------
        add(list, J, easy, "Which keyword is used to inherit a class in Java?",
                "implements", "extends", "inherits", "super", 'B');
        add(list, J, easy, "Which of the following is NOT a primitive data type in Java?",
                "int", "boolean", "String", "char", 'C');
        add(list, J, easy, "What is the default value of an int instance variable in Java?",
                "0", "null", "1", "undefined", 'A');
        add(list, J, easy, "Which method is the starting point of a standalone Java program?",
                "start()", "run()", "begin()", "main()", 'D');
        add(list, J, easy, "Which keyword prevents a class from being extended?",
                "final", "static", "abstract", "const", 'A');

        // ---------- Java: MEDIUM ----------
        add(list, J, med, "Which collection class does NOT allow duplicate elements?",
                "ArrayList", "LinkedList", "HashSet", "Vector", 'C');
        add(list, J, med, "What does System.out.println(10 / 4); print?",
                "2.5", "3", "2.0", "2", 'D');
        add(list, J, med, "Which interface must a class implement so its objects can be serialized?",
                "Cloneable", "Serializable", "Runnable", "Comparable", 'B');
        add(list, J, med, "Which of these is a checked exception?",
                "IOException", "NullPointerException", "ArithmeticException", "IllegalArgumentException", 'A');
        add(list, J, med, "What does the static keyword mean for a method?",
                "It can only be called through an object", "It must return a value",
                "It belongs to the class, not to an object", "It cannot access any variables", 'C');

        // ---------- Java: HARD ----------
        add(list, J, hard, "What does the expression (\"abc\" == new String(\"abc\")) evaluate to?",
                "false", "true", "Compile-time error", "Runtime exception", 'A');
        add(list, J, hard, "Which statement about HashMap is correct?",
                "It is synchronized by default", "It allows one null key and many null values",
                "It keeps keys in insertion order", "It does not allow null values", 'B');
        add(list, J, hard, "What happens if iterator.remove() is called before next()?",
                "It removes the first element", "It throws NoSuchElementException",
                "It does nothing silently", "It throws IllegalStateException", 'D');
        add(list, J, hard, "In try-with-resources, how are the resources closed?",
                "In the order they were declared", "Only if a finally block exists",
                "Automatically, in reverse order of declaration", "Only when an exception occurs", 'C');
        add(list, J, hard, "What is printed by: int x = 5; x = x++ + ++x; System.out.println(x);",
                "10", "12", "11", "13", 'B');

        // ---------- SQL: EASY ----------
        add(list, S, easy, "Which SQL statement is used to retrieve data from a table?",
                "GET", "FETCH", "SELECT", "EXTRACT", 'C');
        add(list, S, easy, "Which clause filters rows based on a condition?",
                "FILTER", "LIMIT", "GROUP BY", "WHERE", 'D');
        add(list, S, easy, "Which clause sorts the result set?",
                "ORDER BY", "SORT BY", "GROUP BY", "ARRANGE", 'A');
        add(list, S, easy, "Which statement removes a table and its structure completely?",
                "DELETE FROM", "DROP TABLE", "TRUNCATE TABLE", "CLEAR TABLE", 'B');
        add(list, S, easy, "Which aggregate function returns the number of rows?",
                "SUM()", "LENGTH()", "COUNT()", "ROWNUM()", 'C');

        // ---------- SQL: MEDIUM ----------
        add(list, S, med, "Which constraint uniquely identifies each row and cannot be NULL?",
                "PRIMARY KEY", "FOREIGN KEY", "CHECK", "DEFAULT", 'A');
        add(list, S, med, "Which join returns only rows that have matching values in both tables?",
                "LEFT JOIN", "RIGHT JOIN", "CROSS JOIN", "INNER JOIN", 'D');
        add(list, S, med, "Which clause filters groups after GROUP BY has been applied?",
                "WHERE", "HAVING", "ORDER BY", "LIMIT", 'B');
        add(list, S, med, "Which condition correctly tests for a missing value?",
                "WHERE col = NULL", "WHERE col == NULL", "WHERE col IS NULL", "WHERE col EQUALS NULL", 'C');
        add(list, S, med, "Which keyword removes duplicate rows from a SELECT result?",
                "UNIQUE", "DIFFERENT", "SINGLE", "DISTINCT", 'D');

        // ---------- SQL: HARD ----------
        add(list, S, hard, "Which clause of a SELECT statement is logically evaluated first?",
                "SELECT", "FROM", "WHERE", "ORDER BY", 'B');
        add(list, S, hard, "What does a LEFT JOIN return?",
                "All left-table rows plus matching right-table rows (NULL if no match)",
                "Only rows that match in both tables",
                "All right-table rows plus matching left-table rows",
                "The Cartesian product of both tables", 'A');
        add(list, S, hard, "Which normal form removes partial dependencies on a composite primary key?",
                "First Normal Form", "Third Normal Form", "Boyce-Codd Normal Form", "Second Normal Form", 'D');
        add(list, S, hard, "How do COUNT(column) and COUNT(*) differ?",
                "They always give the same result",
                "COUNT(column) skips NULLs in that column; COUNT(*) counts all rows",
                "COUNT(*) skips NULLs", "COUNT(column) counts only duplicates", 'B');
        add(list, S, hard, "Which ACID property guarantees that committed data survives a system crash?",
                "Atomicity", "Consistency", "Durability", "Isolation", 'C');

        for (Question q : list) {
            questionDao.insert(q);
        }
        logInfo("Seeder: inserted " + list.size() + " questions");
    }

    /** Small helper so each question stays on a few short lines. */
    private static void add(List<Question> list, String subject, Difficulty difficulty, String text,
                            String a, String b, String c, String d, char correct) {
        list.add(new Question(subject, text, a, b, c, d, correct, difficulty));
    }

    // ---- logging helper: if AppLogger has a different method name, change ONLY this one ----
    private static void logInfo(String message) {
        AppLogger.getLogger().info(message);
    }
}