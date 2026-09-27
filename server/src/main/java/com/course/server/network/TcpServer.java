package com.course.server.network;

import com.course.server.config.ServerConfig;
import com.course.server.session.ClientSession;
import com.course.server.session.SessionManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Core TCP Server managing ServerSocket, thread pool, accept loop, and heartbeat watchdog.
 */
public class TcpServer {
    private static final Logger LOGGER = Logger.getLogger(TcpServer.class.getName());

    private final int port;
    private final int maxClients;
    private final long heartbeatTimeoutMs;

    private ServerSocket serverSocket;
    private ExecutorService clientThreadPool;
    private ScheduledExecutorService watchdogScheduler;
    private Thread acceptThread;

    private volatile boolean running = false;
    private LocalDateTime startedAt;

    public TcpServer(int port, int maxClients, long heartbeatTimeoutMs) {
        this.port = port;
        this.maxClients = maxClients;
        this.heartbeatTimeoutMs = heartbeatTimeoutMs;
    }

    public static TcpServer createDefault() {
        ServerConfig config = ServerConfig.getInstance();
        return new TcpServer(config.getServerPort(), config.getServerMaxClients(), config.getHeartbeatTimeoutMs());
    }

    public synchronized void start() throws IOException {
        if (running) {
            LOGGER.warning("Server is already running on port " + port);
            return;
        }

        this.serverSocket = new ServerSocket(port);
        this.clientThreadPool = Executors.newFixedThreadPool(maxClients);
        this.watchdogScheduler = Executors.newSingleThreadScheduledExecutor();
        this.running = true;
        this.startedAt = LocalDateTime.now();

        LOGGER.info(String.format("=================================================="));
        LOGGER.info(String.format("  TCP Server started successfully on port %d", port));
        LOGGER.info(String.format("  Max concurrent clients: %d", maxClients));
        LOGGER.info(String.format("=================================================="));

        // Start accept loop
        this.acceptThread = new Thread(this::acceptLoop, "TcpServer-AcceptLoop");
        this.acceptThread.start();

        // Start heartbeat watchdog
        this.watchdogScheduler.scheduleAtFixedRate(this::checkHeartbeats, 10, 10, TimeUnit.SECONDS);
    }

    private void acceptLoop() {
        while (running && !serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                // Configure socket parameters
                clientSocket.setKeepAlive(true);
                clientSocket.setTcpNoDelay(true);

                if (SessionManager.getInstance().getOnlineCount() >= maxClients) {
                    LOGGER.warning("Max clients reached. Rejecting connection from " + clientSocket.getRemoteSocketAddress());
                    clientSocket.close();
                    continue;
                }

                ClientHandler handler = new ClientHandler(clientSocket);
                clientThreadPool.submit(handler);
            } catch (IOException e) {
                if (running) {
                    LOGGER.log(Level.WARNING, "Exception in ServerSocket accept loop", e);
                }
            }
        }
    }

    private void checkHeartbeats() {
        if (!running) {
            return;
        }
        try {
            List<ClientSession> sessions = SessionManager.getInstance().getAllSessions();
            for (ClientSession session : sessions) {
                if (session.isTimedOut(heartbeatTimeoutMs)) {
                    LOGGER.warning(String.format("Session %s (User: %s) heartbeat timed out (> %d ms). Evicting.",
                            session.getSessionId(), session.getStudentId(), heartbeatTimeoutMs));
                    session.close();
                    SessionManager.getInstance().removeSession(session.getSessionId());
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error during heartbeat watchdog check", e);
        }
    }

    public synchronized void stop() {
        if (!running) {
            return;
        }
        LOGGER.info("Stopping TCP Server on port " + port + "...");
        this.running = false;

        // 1. Close ServerSocket so accept loop terminates
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error closing ServerSocket", e);
        }

        // 2. Disconnect all active client sessions with message
        SessionManager.getInstance().closeAllSessions();

        // 3. Stop watchdog
        if (watchdogScheduler != null) {
            watchdogScheduler.shutdownNow();
        }

        // 4. Shutdown client thread pool
        if (clientThreadPool != null) {
            clientThreadPool.shutdown();
            try {
                if (!clientThreadPool.awaitTermination(3, TimeUnit.SECONDS)) {
                    clientThreadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                clientThreadPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        LOGGER.info("TCP Server stopped cleanly.");
    }

    public boolean isRunning() {
        return running;
    }

    public int getPort() {
        return port;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }
}
