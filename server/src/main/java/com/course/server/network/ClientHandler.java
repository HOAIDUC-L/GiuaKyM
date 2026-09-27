package com.course.server.network;

import com.course.common.protocol.Action;
import com.course.common.protocol.ErrorCode;
import com.course.common.protocol.Request;
import com.course.common.protocol.Response;
import com.course.common.util.JsonUtil;
import com.course.server.session.ClientSession;
import com.course.server.session.SessionManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles communication with a single connected client over TCP Socket.
 * Runs on a dedicated thread managed by ExecutorService.
 */
public class ClientHandler implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(ClientHandler.class.getName());

    private final Socket socket;
    private final String sessionId;
    private ClientSession session;
    private BufferedReader reader;
    private PrintWriter writer;
    private volatile boolean running = true;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.sessionId = "SESS-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Override
    public void run() {
        try {
            // Setup UTF-8 streams
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            // Register session
            this.session = new ClientSession(sessionId, socket, writer);
            SessionManager.getInstance().addSession(session);

            LOGGER.info(String.format("Client connected from %s:%d (Session ID: %s)",
                    session.getClientIp(), session.getClientPort(), sessionId));

            String line;
            while (running && (line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                try {
                    Request request = JsonUtil.fromJson(line, Request.class);
                    if (request == null) {
                        session.sendResponse(Response.error(Action.ERROR, null, ErrorCode.INVALID_REQUEST, "Gói tin không đúng định dạng JSON."));
                        continue;
                    }

                    // Process request via ProtocolHandler
                    Response response = ProtocolHandler.getInstance().handle(session, request);
                    if (response != null) {
                        session.sendResponse(response);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error processing client message: " + line, e);
                    session.sendResponse(Response.error(Action.ERROR, null, ErrorCode.INVALID_REQUEST, "Lỗi phân tích cú pháp JSON: " + e.getMessage()));
                }
            }
        } catch (SocketException e) {
            LOGGER.info(String.format("Client disconnected abruptly (Session: %s, Error: %s)", sessionId, e.getMessage()));
        } catch (IOException e) {
            LOGGER.log(Level.INFO, "I/O error on session " + sessionId, e);
        } finally {
            cleanup();
        }
    }

    public void stop() {
        this.running = false;
        cleanup();
    }

    private synchronized void cleanup() {
        running = false;
        if (session != null) {
            SessionManager.getInstance().removeSession(sessionId);
        }
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (IOException ignored) {
        }
        if (writer != null) {
            writer.close();
        }
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
        LOGGER.fine("Cleaned up resources for session " + sessionId);
    }

    public String getSessionId() {
        return sessionId;
    }

    public ClientSession getSession() {
        return session;
    }
}
