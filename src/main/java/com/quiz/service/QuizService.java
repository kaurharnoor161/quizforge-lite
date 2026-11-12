package com.quiz.service;

import com.quiz.dao.JdbcQuestionDAO;
import com.quiz.dao.JdbcResultDAO;
import com.quiz.dao.QuestionDAO;
import com.quiz.dao.ResultDAO;
import com.quiz.exception.NotEnoughQuestionsException;
import com.quiz.model.Question;
import com.quiz.model.Result;
import com.quiz.util.AppLogger;
import com.quiz.util.ConfigLoader;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Logic of one quiz (no JavaFX here): pick questions, score the answers, save the result.
 *
 * The student's answers are kept in a HashMap<Integer, Character>:
 * key = question id, value = the chosen option ('A'..'D').
 * A HashMap gives a fast lookup by question id, and a question that is missing from the map
 * simply means "not answered".
 */
public class QuizService {

    private final QuestionDAO questionDao;
    private final ResultDAO resultDao;

    public QuizService(QuestionDAO questionDao, ResultDAO resultDao) {
        this.questionDao = Objects.requireNonNull(questionDao, "questionDao must not be null");
        this.resultDao = Objects.requireNonNull(resultDao, "resultDao must not be null");
    }

    public QuizService() {
        this(new JdbcQuestionDAO(), new JdbcResultDAO());
    }

    /** Builds the question paper for one quiz. */
    public List<Question> buildPaper(String subject, boolean shuffle) throws NotEnoughQuestionsException {
        int n = ConfigLoader.getInt("questions.per.quiz");
        try {
            int available = questionDao.countBySubject(subject);
            if (available < n) {
                throw new NotEnoughQuestionsException("The subject \"" + subject + "\" has only "
                        + available + " questions, but a quiz needs " + n + ".");
            }

            List<Question> picked = questionDao.findRandomBySubject(subject, n);
            if (picked.size() < n) {
                throw new NotEnoughQuestionsException("The subject \"" + subject + "\" has only "
                        + picked.size() + " questions, but a quiz needs " + n + ".");
            }
            return arrangePaper(picked, shuffle);
        } catch (SQLException e) {
            AppLogger.severe("Could not build a quiz paper for subject " + subject, e);
            throw new IllegalStateException("Database error", e);
        }
    }

    /**
     * Puts the picked questions in their final order and returns a NEW list.
     * The database already picks the questions at random, so "shuffle" only decides whether
     * they are shown in random order (true) or in a steady order by question id (false).
     */
    public static List<Question> arrangePaper(List<Question> picked, boolean shuffle) {
        Objects.requireNonNull(picked, "picked must not be null");

        List<Question> paper = new ArrayList<>(picked); // copy: the input list is never changed
        if (shuffle) {
            Collections.shuffle(paper);
        } else {
            paper.sort(new Comparator<Question>() {
                @Override
                public int compare(Question a, Question b) {
                    return Integer.compare(a.getQuestionId(), b.getQuestionId());
                }
            });
        }
        return paper;
    }

    /** How many answers are correct. Unanswered questions and unknown ids do not count. */
    public static int countCorrect(List<Question> paper, Map<Integer, Character> answers) {
        Objects.requireNonNull(paper, "paper must not be null");
        Objects.requireNonNull(answers, "answers must not be null");

        int correct = 0;
        for (Question q : paper) {
            if (q == null) {
                continue;
            }
            Character chosen = answers.get(q.getQuestionId());
            if (chosen != null && chosen.charValue() == q.getCorrectOption()) {
                correct++;
            }
        }
        return correct;
    }

    /** How many questions of the paper have no answer in the map. */
    public static int countUnanswered(List<Question> paper, Map<Integer, Character> answers) {
        Objects.requireNonNull(paper, "paper must not be null");
        Objects.requireNonNull(answers, "answers must not be null");

        int unanswered = 0;
        for (Question q : paper) {
            if (q != null && answers.get(q.getQuestionId()) == null) {
                unanswered++;
            }
        }
        return unanswered;
    }

    /** Pure scoring: no database access. Returns a NEW Result with id 0. */
    public Result gradeQuiz(int userId, String subject, List<Question> paper, Map<Integer, Character> answers) {
        Objects.requireNonNull(paper, "paper must not be null");
        Objects.requireNonNull(answers, "answers must not be null");
        if (paper.isEmpty()) {
            throw new IllegalArgumentException("The paper has no questions");
        }

        int score = countCorrect(paper, answers);
        // withNano(0) keeps the stored text in a clean ISO format like 2026-11-12T10:15:30
        return new Result(userId, subject, score, paper.size(), LocalDateTime.now().withNano(0));
    }

    /** Scores the quiz and saves the result. Returns the saved Result (with its new id). */
    public Result submitQuiz(int userId, String subject, List<Question> paper, Map<Integer, Character> answers) {
        Result result = gradeQuiz(userId, subject, paper, answers);
        try {
            resultDao.insert(result); // the DAO sets the generated id on the object
            AppLogger.info("User " + userId + " finished " + subject + ": "
                    + result.getScore() + "/" + result.getTotal());
            return result;
        } catch (SQLException e) {
            AppLogger.severe("Could not save the result of user " + userId + " for subject " + subject, e);
            throw new IllegalStateException("Database error", e);
        }
    }
}