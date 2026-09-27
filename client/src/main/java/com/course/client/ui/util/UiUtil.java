package com.course.client.ui.util;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * UI utility for setting modern Look and Feel, fonts, and dialogs.
 */
public final class UiUtil {
    private static final Logger LOGGER = Logger.getLogger(UiUtil.class.getName());

    public static final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font REGULAR_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font SMALL_FONT = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font MONO_FONT = new Font("Consolas", Font.PLAIN, 12);

    public static final Color PRIMARY_COLOR = new Color(24, 119, 242);
    public static final Color SUCCESS_COLOR = new Color(46, 125, 50);
    public static final Color DANGER_COLOR = new Color(198, 40, 40);
    public static final Color WARNING_COLOR = new Color(239, 108, 0);

    private UiUtil() {
    }

    public static void setupLookAndFeel() {
        try {
            FlatLightLaf.setup();
        } catch (Throwable t) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to set system look and feel", e);
            }
        }

        // Set default font globally
        UIManager.put("defaultFont", REGULAR_FONT);
    }

    public static void showError(Component parent, String message, String title) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE)
        );
    }

    public static void showInfo(Component parent, String message, String title) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE)
        );
    }

    public static void showWarning(Component parent, String message, String title) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(parent, message, title, JOptionPane.WARNING_MESSAGE)
        );
    }
}
