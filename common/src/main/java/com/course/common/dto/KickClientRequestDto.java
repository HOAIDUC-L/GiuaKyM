package com.course.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class KickClientRequestDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String studentId;
    private String sessionId;
    private String reason;

    public KickClientRequestDto() {
    }

    public KickClientRequestDto(String studentId, String sessionId, String reason) {
        this.studentId = studentId;
        this.sessionId = sessionId;
        this.reason = reason;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
