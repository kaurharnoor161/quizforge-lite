package com.quiz.util;

import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * One shared logger for the whole app (java.util.logging only).
 * Writes every message to the console AND to the log file from config.properties.
 */
public final class AppLogger {

    private static final Logger LOGGER = Logger.getLogger("QuizForge");

    // static block: runs once, the first time AppLogger is used
    static {
        setup();
    }

    private AppLogger() {
    }

    private static void setup() {
        // Short one-line format: 2025-01-31 10:15:30 [INFO] message
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "%1$tF %1$tT [%4$s] %5$s%6$s%n");

        LOGGER.setUseParentHandlers(false); // avoid printing every line twice
        LOGGER.setLevel(Level.ALL);

        // 1) Console output
        ConsoleHandler console = new ConsoleHandler();
        console.setLevel(Level.ALL);
        console.setFormatter(new SimpleFormatter());
        LOGGER.addHandler(console);

        // 2) File output (true = append to the existing file)
        try {
            String logFile = ConfigLoader.getString("log.file");
            FileHandler file = new FileHandler(logFile, true);
            file.setLevel(Level.ALL);
            file.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(file);
        } catch (IOException | RuntimeException e) {
            // If the file cannot be opened we still keep logging to the console
            LOGGER.log(Level.WARNING, "Could not open log file, using console only", e);
        }
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    public static void info(String message) {
        LOGGER.info(message);
    }

    public static void warning(String message) {
        LOGGER.warning(message);
    }

    public static void severe(String message, Throwable error) {
        LOGGER.log(Level.SEVERE, message, error);
    }
}