package com.quiz.service;

import com.quiz.dao.JdbcQuestionDAO;
import com.quiz.dao.QuestionDAO;
import com.quiz.model.Question;
import com.quiz.util.AppLogger;
import com.quiz.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/** Backup and restore of the question bank (no JavaFX). */
public class BackupService {

    private final QuestionDAO questionDao;

    public BackupService(QuestionDAO questionDao) {
        this.questionDao = Objects.requireNonNull(questionDao, "questionDao must not be null");
    }

    public BackupService() {
        this(new JdbcQuestionDAO());
    }

    /** Saves all questions to a file and returns how many were saved. */
    public int backupQuestions(File file) throws SQLException, IOException {
        List<Question> questions = questionDao.findAll();
        FileUtil.writeBackup(questions, file);
        AppLogger.info("Question backup written: " + questions.size()
                + " question(s) to " + file.getAbsolutePath());
        return questions.size();
    }

    /** Replaces the whole question bank with the questions from a backup file. */
    public RestoreSummary restoreQuestions(File file)
            throws SQLException, IOException, ClassNotFoundException {

        // 1. read the list from the file
        List<Question> questions = FileUtil.readBackup(file);

        // 2. clean the list with an Iterator
        // iterator.remove() is the safe way to delete while looping. Calling questions.remove(...)
        // inside a for-each loop changes the list behind the loop's back and throws
        // ConcurrentModificationException.
        int skipped = 0;
        Iterator<Question> it = questions.iterator();
        while (it.hasNext()) {
            Question q = it.next();
            if (q == null || !q.isValid()) {
                it.remove();
                skipped++;
            }
        }

        // 3. never wipe the bank with an empty or damaged backup: stop BEFORE touching the database
        if (questions.isEmpty()) {
            throw new IllegalArgumentException(
                    "The backup contains no valid questions. Nothing was changed.");
        }

        // 4. delete all + insert all inside ONE transaction (built in Step 3)
        questionDao.replaceAll(questions);

        // 5. log and report
        AppLogger.info("Question restore finished from " + file.getAbsolutePath()
                + ": restored=" + questions.size() + ", skipped=" + skipped);
        return new RestoreSummary(questions.size(), skipped);
    }

    /** Small result object: how many questions were restored and how many were skipped. */
    public static class RestoreSummary {
        private final int restoredCount;
        private final int skippedCount;

        public RestoreSummary(int restoredCount, int skippedCount) {
            this.restoredCount = restoredCount;
            this.skippedCount = skippedCount;
        }

        public int getRestoredCount() {
            return restoredCount;
        }

        public int getSkippedCount() {
            return skippedCount;
        }
    }
}