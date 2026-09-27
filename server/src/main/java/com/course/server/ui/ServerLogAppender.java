package com.course.server.ui;

import javax.swing.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * Custom logging handler capturing server logs to display on the Admin Dashboard console.
 */
public class ServerLogAppender extends Handler {
    private final JTextArea logArea;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    public ServerLogAppender(JTextArea logArea) {
        this.logArea = logArea;
    }

    @Override
    public void publish(LogRecord record) {
        if (record == null || logArea == null) {
            return;
        }

        String time = LocalDateTime.now().format(formatter);
        String level = record.getLevel().getName();
        String message = record.getMessage();

        String line = String.format("[%s] [%-7s] %s\n", time, level, message);

        SwingUtilities.invokeLater(() -> {
            logArea.append(line);
            // Limit buffer size to 5000 lines
            if (logArea.getLineCount() > 5000) {
                try {
                    int endPos = logArea.getLineEndOffset(1000);
                    logArea.replaceRange("", 0, endPos);
                } catch (Exception ignored) {
                }
            }
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() throws SecurityException {
    }
}
