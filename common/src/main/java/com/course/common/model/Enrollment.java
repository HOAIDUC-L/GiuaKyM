package com.course.common.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entity representing an enrollment link between a Student and a Course.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Enrollment implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long enrollmentId;
    private String studentId;
    private String courseId;
    private LocalDateTime registeredAt;

    public Enrollment() {
    }

    public Enrollment(Long enrollmentId, String studentId, String courseId, LocalDateTime registeredAt) {
        this.enrollmentId = enrollmentId;
        this.studentId = studentId;
        this.courseId = courseId;
        this.registeredAt = registeredAt;
    }

    public Long getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(Long enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }

    @Override
    public String toString() {
        return "Enrollment{" +
                "enrollmentId=" + enrollmentId +
                ", studentId='" + studentId + '\'' +
                ", courseId='" + courseId + '\'' +
                ", registeredAt=" + registeredAt +
                '}';
    }
}
