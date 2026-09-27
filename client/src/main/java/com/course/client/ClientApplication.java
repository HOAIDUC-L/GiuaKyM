package com.course.client;

import java.awt.EventQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main application entry point for the Client module.
 */
public class ClientApplication {
    private static final Logger LOGGER = Logger.getLogger(ClientApplication.class.getName());

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                // If GUI LoginFrame is available (Part 7), launch it.
                Class<?> loginFrameClass = Class.forName("com.course.client.ui.LoginFrame");
                Object frame = loginFrameClass.getDeclaredConstructor().newInstance();
                loginFrameClass.getMethod("setVisible", boolean.class).invoke(frame, true);
            } catch (ClassNotFoundException e) {
                LOGGER.info("Client UI not yet loaded (Part 7 pending). Client networking is ready.");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to launch Client UI", e);
            }
        });
    }
}
