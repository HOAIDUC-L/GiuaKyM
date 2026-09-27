package com.course.client.config;

import com.course.common.constants.AppConstants;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads client configuration from application.properties.
 * Strictly contains NO database credentials or JDBC configs.
 */
public class ClientConfig {
    private static final Logger LOGGER = Logger.getLogger(ClientConfig.class.getName());
    private static final ClientConfig INSTANCE = new ClientConfig();

    private String serverHost;
    private int serverPort;
    private long pingIntervalMs;
    private long requestTimeoutMs;

    private ClientConfig() {
        loadProperties();
    }

    public static ClientConfig getInstance() {
        return INSTANCE;
    }

    private void loadProperties() {
        Properties props = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) {
                props.load(in);
            } else {
                LOGGER.warning("application.properties not found in client classpath, using defaults.");
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load client application.properties", e);
        }

        this.serverHost = props.getProperty("server.host", AppConstants.DEFAULT_HOST);
        this.serverPort = Integer.parseInt(props.getProperty("server.port", String.valueOf(AppConstants.DEFAULT_PORT)));
        this.pingIntervalMs = Long.parseLong(props.getProperty("client.ping.interval-ms", "10000"));
        this.requestTimeoutMs = Long.parseLong(props.getProperty("client.request.timeout-ms", "10000"));
    }

    public String getServerHost() {
        return serverHost;
    }

    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public long getPingIntervalMs() {
        return pingIntervalMs;
    }

    public long getRequestTimeoutMs() {
        return requestTimeoutMs;
    }
}
