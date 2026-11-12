package com.quiz.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

/**
 * Loads config.properties from the classpath (src/main/resources)
 * and gives easy access to the values.
 */
public final class ConfigLoader {

    private static final String FILE_NAME = "config.properties";
    private static final Properties PROPS = new Properties();
    private static boolean loaded = false;

    // Private constructor: this is a utility class, nobody should create objects of it
    private ConfigLoader() {
    }

    /** Reads the properties file once. Safe to call many times. */
    public static synchronized void load() {
        if (loaded) {
            return;
        }
        // try-with-resources closes the stream automatically
        try (InputStream in = ConfigLoader.class.getClassLoader().getResourceAsStream(FILE_NAME)) {
            if (in == null) {
                throw new IllegalStateException(FILE_NAME + " not found on the classpath");
            }
            PROPS.load(in);
            loaded = true;
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + FILE_NAME, e);
        }
    }

    /** Returns the text value for a key, e.g. getString("db.path"). */
    public static String getString(String key) {
        Objects.requireNonNull(key, "key must not be null");
        load(); // make sure the file has been read
        String value = PROPS.getProperty(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing key in " + FILE_NAME + ": " + key);
        }
        return value.trim();
    }

    /** Returns the value for a key converted to int, e.g. getInt("questions.per.quiz"). */
    public static int getInt(String key) {
        String text = getString(key);
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Key '" + key + "' is not a number: " + text, e);
        }
    }
}