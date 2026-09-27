package com.course.client.network;

import com.course.client.config.ClientConfig;
import com.course.common.dto.CourseUpdatedPushDto;
import com.course.common.protocol.Action;
import com.course.common.protocol.ErrorCode;
import com.course.common.protocol.Request;
import com.course.common.protocol.Response;
import com.course.common.util.JsonUtil;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the TCP Socket connection to the Server.
 * Implements a Dedicated Reader Thread for server push handling
 * and CompletableFuture correlation for asynchronous/synchronous request-response matching.
 */
public class ClientSocketManager {
    private static final Logger LOGGER = Logger.getLogger(ClientSocketManager.class.getName());
    private static final ClientSocketManager INSTANCE = new ClientSocketManager();

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    private Thread readerThread;
    private ScheduledExecutorService heartbeatScheduler;
    private volatile boolean running = false;

    // RequestId -> CompletableFuture for synchronous/asynchronous waiting
    private final Map<String, CompletableFuture<Response>> pendingRequests = new ConcurrentHashMap<>();

    // Push event listeners
    private final List<ServerPushListener> pushListeners = new CopyOnWriteArrayList<>();

    private ClientSocketManager() {
    }

    public static ClientSocketManager getInstance() {
        return INSTANCE;
    }

    public void addPushListener(ServerPushListener listener) {
        if (listener != null && !pushListeners.contains(listener)) {
            pushListeners.add(listener);
        }
    }

    public void removePushListener(ServerPushListener listener) {
        pushListeners.remove(listener);
    }

    /**
     * Connects to the server.
     */
    public synchronized void connect(String host, int port) throws IOException {
        if (isConnected()) {
            return;
        }

        LOGGER.info(String.format("Connecting to server at %s:%d...", host, port));
        this.socket = new Socket();
        this.socket.connect(new InetSocketAddress(host, port), 5000);
        this.socket.setTcpNoDelay(true);
        this.socket.setKeepAlive(true);

        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

        this.running = true;

        // 1. Start Dedicated Reader Thread
        this.readerThread = new Thread(this::listenServerMessages, "Client-DedicatedReaderThread");
        this.readerThread.setDaemon(true);
        this.readerThread.start();

        // 2. Start periodic Heartbeat (PING)
        long pingInterval = ClientConfig.getInstance().getPingIntervalMs();
        this.heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
        this.heartbeatScheduler.scheduleAtFixedRate(this::sendHeartbeat, pingInterval, pingInterval, TimeUnit.MILLISECONDS);

        LOGGER.info("Connected to server successfully.");
        for (ServerPushListener listener : pushListeners) {
            listener.onConnected();
        }
    }

    /**
     * Dedicated reader thread loop. Continuously consumes server JSON frames.
     */
    private void listenServerMessages() {
        try {
            String line;
            while (running && (line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                try {
                    Response response = JsonUtil.fromJson(line, Response.class);
                    if (response != null) {
                        handleIncomingResponse(response);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to parse incoming response: " + line, e);
                }
            }
        } catch (SocketException e) {
            if (running) {
                LOGGER.info("Socket connection terminated: " + e.getMessage());
            }
        } catch (IOException e) {
            if (running) {
                LOGGER.log(Level.WARNING, "I/O error in reader thread", e);
            }
        } finally {
            if (running) {
                notifyDisconnected("Mất kết nối với máy chủ.");
            }
            cleanup();
        }
    }

    private void handleIncomingResponse(Response response) {
        String reqId = response.getRequestId();

        // 1. Check if this is a response to an awaiting request
        if (reqId != null && pendingRequests.containsKey(reqId)) {
            CompletableFuture<Response> future = pendingRequests.remove(reqId);
            if (future != null) {
                future.complete(response);
                return;
            }
        }

        // 2. Handle Server Push Actions
        Action action = response.getAction();
        if (action == Action.COURSE_UPDATED) {
            CourseUpdatedPushDto dto = response.getDataAs(CourseUpdatedPushDto.class);
            if (dto != null) {
                LOGGER.info(String.format("Received broadcast COURSE_UPDATED: Course %s -> %d slots",
                        dto.getCourseId(), dto.getRemainingSlots()));
                for (ServerPushListener listener : pushListeners) {
                    try {
                        listener.onCourseUpdated(dto.getCourseId(), dto.getRemainingSlots());
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Error in pushListener onCourseUpdated", e);
                    }
                }
            }
        } else if (action == Action.FORCE_LOGOUT) {
            String message = response.getMessage() != null ? response.getMessage() : "Bạn đã bị ngắt kết nối bởi máy chủ.";
            LOGGER.warning("Received FORCE_LOGOUT: " + message);
            for (ServerPushListener listener : pushListeners) {
                try {
                    listener.onForceLogout(message);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error in pushListener onForceLogout", e);
                }
            }
            disconnect();
        } else if (action == Action.PONG) {
            LOGGER.fine("Heartbeat PONG received from server.");
        }
    }

    /**
     * Sends an asynchronous request, returning a CompletableFuture.
     */
    public CompletableFuture<Response> sendRequestAsync(Request request) {
        if (!isConnected()) {
            CompletableFuture<Response> failed = new CompletableFuture<>();
            failed.complete(Response.error(request.getAction(), request.getRequestId(), ErrorCode.SERVER_ERROR, "Chưa kết nối đến máy chủ."));
            return failed;
        }

        CompletableFuture<Response> future = new CompletableFuture<>();
        pendingRequests.put(request.getRequestId(), future);

        try {
            String json = JsonUtil.toJson(request);
            synchronized (this) {
                writer.println(json);
                writer.flush();
            }
        } catch (Exception e) {
            pendingRequests.remove(request.getRequestId());
            future.completeExceptionally(e);
        }

        return future;
    }

    /**
     * Sends a synchronous request with a timeout.
     */
    public Response sendRequest(Request request, long timeoutMs) {
        try {
            CompletableFuture<Response> future = sendRequestAsync(request);
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            pendingRequests.remove(request.getRequestId());
            LOGGER.warning("Request timed out for requestId: " + request.getRequestId());
            return Response.error(request.getAction(), request.getRequestId(), ErrorCode.SERVER_ERROR, "Yêu cầu quá thời gian chờ phản hồi từ máy chủ.");
        } catch (Exception e) {
            pendingRequests.remove(request.getRequestId());
            LOGGER.log(Level.WARNING, "Error executing request: " + request.getRequestId(), e);
            return Response.error(request.getAction(), request.getRequestId(), ErrorCode.SERVER_ERROR, "Lỗi kết nối mạng: " + e.getMessage());
        }
    }

    private void sendHeartbeat() {
        if (isConnected()) {
            Request ping = Request.of(Action.PING, "PING");
            sendRequestAsync(ping);
        }
    }

    private void notifyDisconnected(String reason) {
        for (ServerPushListener listener : pushListeners) {
            try {
                listener.onDisconnected(reason);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error in pushListener onDisconnected", e);
            }
        }
    }

    public synchronized void disconnect() {
        if (!running) {
            return;
        }
        running = false;
        cleanup();
    }

    private synchronized void cleanup() {
        running = false;
        if (heartbeatScheduler != null) {
            heartbeatScheduler.shutdownNow();
        }

        // Fail any pending futures
        for (Map.Entry<String, CompletableFuture<Response>> entry : pendingRequests.entrySet()) {
            entry.getValue().complete(Response.error(Action.ERROR, entry.getKey(), ErrorCode.SERVER_ERROR, "Mất kết nối máy chủ."));
        }
        pendingRequests.clear();

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
    }

    public boolean isConnected() {
        return running && socket != null && socket.isConnected() && !socket.isClosed();
    }
}
