package com.course.common.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entity representing an audit log entry for client sessions.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionLog implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long sessionId;
    private String studentId;
    private String clientIp;
    private Integer clientPort;
    private LocalDateTime loginTime;
    private LocalDateTime logoutTime;
    private String status; // ONLINE, LOGOUT, KICKED, DISCONNECTED

    public SessionLog() {
    }

    public SessionLog(Long sessionId, String studentId, String clientIp, Integer clientPort, LocalDateTime loginTime, LocalDateTime logoutTime, String status) {
        this.sessionId = sessionId;
        this.studentId = studentId;
        this.clientIp = clientIp;
        this.clientPort = clientPort;
        this.loginTime = loginTime;
        this.logoutTime = logoutTime;
        this.status = status;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public Integer getClientPort() {
        return clientPort;
    }

    public void setClientPort(Integer clientPort) {
        this.clientPort = clientPort;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }

    public LocalDateTime getLogoutTime() {
        return logoutTime;
    }

    public void setLogoutTime(LocalDateTime logoutTime) {
        this.logoutTime = logoutTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "SessionLog{" +
                "sessionId=" + sessionId +
                ", studentId='" + studentId + '\'' +
                ", clientIp='" + clientIp + '\'' +
                ", clientPort=" + clientPort +
                ", loginTime=" + loginTime +
                ", logoutTime=" + logoutTime +
                ", status='" + status + '\'' +
                '}';
    }
}
