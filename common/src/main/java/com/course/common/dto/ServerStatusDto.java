package com.course.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ServerStatusDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean running;
    private int port;
    private int onlineClientsCount;
    private int maxClients;
    private LocalDateTime startedAt;

    public ServerStatusDto() {
    }

    public ServerStatusDto(boolean running, int port, int onlineClientsCount, int maxClients, LocalDateTime startedAt) {
        this.running = running;
        this.port = port;
        this.onlineClientsCount = onlineClientsCount;
        this.maxClients = maxClients;
        this.startedAt = startedAt;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getOnlineClientsCount() {
        return onlineClientsCount;
    }

    public void setOnlineClientsCount(int onlineClientsCount) {
        this.onlineClientsCount = onlineClientsCount;
    }

    public int getMaxClients() {
        return maxClients;
    }

    public void setMaxClients(int maxClients) {
        this.maxClients = maxClients;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }
}
