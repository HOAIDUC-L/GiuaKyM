package com.course.common.dto;

import com.course.common.constants.AppConstants;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Detailed view of an enrollment including course information.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnrollmentDetailDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long enrollmentId;
    private String studentId;
    private String courseId;
    private String subjectCode;
    private String courseName;
    private String lecturerName;
    private int dayOfWeek;
    private int startPeriod;
    private int endPeriod;
    private String room;
    private LocalDateTime registeredAt;

    public EnrollmentDetailDto() {
    }

    public EnrollmentDetailDto(Long enrollmentId, String studentId, String courseId, String subjectCode,
                               String courseName, String lecturerName, int dayOfWeek,
                               int startPeriod, int endPeriod, String room, LocalDateTime registeredAt) {
        this.enrollmentId = enrollmentId;
        this.studentId = studentId;
        this.courseId = courseId;
        this.subjectCode = subjectCode;
        this.courseName = courseName;
        this.lecturerName = lecturerName;
        this.dayOfWeek = dayOfWeek;
        this.startPeriod = startPeriod;
        this.endPeriod = endPeriod;
        this.room = room;
        this.registeredAt = registeredAt;
    }

    public String getScheduleDisplay() {
        return AppConstants.getDayName(dayOfWeek) + ", Tiết " + startPeriod + " - " + endPeriod;
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

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getLecturerName() {
        return lecturerName;
    }

    public void setLecturerName(String lecturerName) {
        this.lecturerName = lecturerName;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(int dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public int getStartPeriod() {
        return startPeriod;
    }

    public void setStartPeriod(int startPeriod) {
        this.startPeriod = startPeriod;
    }

    public int getEndPeriod() {
        return endPeriod;
    }

    public void setEndPeriod(int endPeriod) {
        this.endPeriod = endPeriod;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }
}
