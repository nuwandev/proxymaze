package com.binarybeasts.util;

import org.slf4j.Logger;

public class LogHighlighter {

    // ANSI color codes
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BRIGHT_GREEN = "\u001B[92m";
    private static final String ANSI_BRIGHT_YELLOW = "\u001B[93m";
    private static final String ANSI_BRIGHT_RED = "\u001B[91m";
    private static final String ANSI_BRIGHT_CYAN = "\u001B[96m";

    /**
     * Log an info message with highlight marker
     */
    public static void info(Logger logger, String category, String message, Object... args) {
        String highlighted = String.format("%s[█ %s] %s%s", ANSI_BRIGHT_GREEN, category, message, ANSI_RESET);
        logger.info(highlighted, args);
    }

    /**
     * Log a warning message with highlight marker
     */
    public static void warn(Logger logger, String category, String message, Object... args) {
        String highlighted = String.format("%s[⚠ %s] %s%s", ANSI_BRIGHT_YELLOW, category, message, ANSI_RESET);
        logger.warn(highlighted, args);
    }

    /**
     * Log an error message with highlight marker
     */
    public static void error(Logger logger, String category, String message, Object... args) {
        String highlighted = String.format("%s[✗ %s] %s%s", ANSI_BRIGHT_RED, category, message, ANSI_RESET);
        logger.error(highlighted, args);
    }

    /**
     * Log a debug message with highlight marker
     */
    public static void debug(Logger logger, String category, String message, Object... args) {
        String highlighted = String.format("%s[◆ %s] %s%s", ANSI_BRIGHT_CYAN, category, message, ANSI_RESET);
        logger.debug(highlighted, args);
    }
}

