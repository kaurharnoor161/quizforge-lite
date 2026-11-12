package com.quiz.util;

import com.quiz.model.Question;
import com.quiz.model.Result;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * File helper methods (no JavaFX here).
 *
 * BYTE STREAM vs CHARACTER STREAM (simple version):
 *  - A byte stream (FileOutputStream, ObjectOutputStream) moves raw bytes. It does not know about letters.
 *  - A character stream (Writer / BufferedWriter) moves text. OutputStreamWriter is the bridge:
 *    it turns characters into bytes using an encoding (we use UTF-8).
 *  - "Buffered" means data is collected in memory and written in bigger chunks, which is faster.
 *
 * SERIALIZATION: turns a Java object (here a list of Question objects) into bytes so it can be
 * saved in a file, and later turns those bytes back into real objects. Question implements Serializable.
 */
public final class FileUtil {

    // Only our own model classes and basic java.util / java.lang classes may be read from a backup file.
    // This blocks a hand-made file from creating unexpected objects (a safety rule for deserialization).
    private static final String BACKUP_FILTER = "com.quiz.model.*;java.util.*;java.lang.*;!*";

    private static final String NOT_A_BACKUP = "This is not a valid QuizForge backup file";

    private FileUtil() {
        // utility class: nobody should create an object of it
    }

    /** Writes all results to a CSV file (header line + one line per result). */
    public static void writeResultsCsv(List<Result> results, File file) throws IOException {
        // byte stream -> bridge (UTF-8) -> character stream with buffer
        try (FileOutputStream fileOut = new FileOutputStream(file);
             OutputStreamWriter bridge = new OutputStreamWriter(fileOut, StandardCharsets.UTF_8);
             BufferedWriter writer = new BufferedWriter(bridge)) {

            writer.write("Student,Subject,Score,Total,Percentage,Attempted On");
            writer.newLine();

            for (Result r : results) {
                String line = csvEscape(r.getStudentName()) + ","
                        + csvEscape(r.getSubject()) + ","
                        + r.getScore() + ","
                        + r.getTotal() + ","
                        + r.getPercentage() + ","
                        + csvEscape(r.getAttemptedOnText());
                writer.write(line);
                writer.newLine();
            }
        }
    }

    /*
     * Two rules for every text cell:
     * 1) Formula injection protection: Excel/Numbers treat a cell starting with = + - or @ as a formula.
     *    A student could register as "=HYPERLINK(...)", so we put a ' in front to make it plain text.
     * 2) CSV quoting: if the text has a comma, a double quote or a line break, wrap it in double quotes
     *    and double every inner quote ("Say ""Hi"""), otherwise the columns would be shifted.
     */
    private static String csvEscape(String text) {
        if (text == null) {
            return "";
        }
        String safe = text;
        if (!safe.isEmpty()) {
            char first = safe.charAt(0);
            if (first == '=' || first == '+' || first == '-' || first == '@') {
                safe = "'" + safe;
            }
        }
        if (safe.contains(",") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r")) {
            safe = "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }

    /** Saves the questions to a binary backup file using serialization. */
    public static void writeBackup(List<Question> questions, File file) throws IOException {
        try (FileOutputStream fileOut = new FileOutputStream(file);
             BufferedOutputStream bufferedOut = new BufferedOutputStream(fileOut);
             ObjectOutputStream objectOut = new ObjectOutputStream(bufferedOut)) {

            // A fresh ArrayList is used because ArrayList is Serializable for sure
            objectOut.writeObject(new ArrayList<>(questions));
        }
    }

    /** Reads a backup file. Null elements are kept here and cleaned later by BackupService. */
    public static List<Question> readBackup(File file) throws IOException, ClassNotFoundException {
        try (FileInputStream fileIn = new FileInputStream(file);
             BufferedInputStream bufferedIn = new BufferedInputStream(fileIn);
             ObjectInputStream objectIn = new ObjectInputStream(bufferedIn)) {

            objectIn.setObjectInputFilter(ObjectInputFilter.Config.createFilter(BACKUP_FILTER));

            Object loaded = objectIn.readObject();
            if (!(loaded instanceof List<?>)) {
                throw new IOException(NOT_A_BACKUP);
            }

            List<?> rawList = (List<?>) loaded;
            List<Question> questions = new ArrayList<Question>();
            for (Object item : rawList) {
                if (item != null && !(item instanceof Question)) {
                    throw new IOException(NOT_A_BACKUP);
                }
                questions.add((Question) item); // null stays null on purpose
            }
            return questions;

        } catch (ObjectStreamException | EOFException e) {
            // wrong header (for example a CSV or text file), rejected class, or an empty file
            throw new IOException(NOT_A_BACKUP, e);
        }
    }
}