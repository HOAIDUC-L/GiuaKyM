package com.course.common.dto;

import com.course.common.model.Role;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO representing an active client session for Admin monitoring.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientSessionDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String sessionId;
    private String studentId;
    private String username;
    private String email;
    private String fullName;
    private Role role;
    private String clientIp;
    private int clientPort;
    private LocalDateTime loginTime;
    private long onlineDurationSeconds;
    private String status;

    public ClientSessionDto() {
    }

    public ClientSessionDto(String sessionId, String studentId, String username, String email, String fullName,
                            Role role, String clientIp, int clientPort, LocalDateTime loginTime,
                            long onlineDurationSeconds, String status) {
        this.sessionId = sessionId;
        this.studentId = studentId;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.clientIp = clientIp;
        this.clientPort = clientPort;
        this.loginTime = loginTime;
        this.onlineDurationSeconds = onlineDurationSeconds;
        this.status = status;
    }

    public String getFormattedDuration() {
        long hours = onlineDurationSeconds / 3600;
        long minutes = (onlineDurationSeconds % 3600) / 60;
        long seconds = onlineDurationSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public int getClientPort() {
        return clientPort;
    }

    public void setClientPort(int clientPort) {
        this.clientPort = clientPort;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }

    public long getOnlineDurationSeconds() {
        return onlineDurationSeconds;
    }

    public void setOnlineDurationSeconds(long onlineDurationSeconds) {
        this.onlineDurationSeconds = onlineDurationSeconds;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
