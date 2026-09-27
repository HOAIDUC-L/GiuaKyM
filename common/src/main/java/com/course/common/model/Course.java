package com.course.common.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entity representing a course offering with its schedule and slot availability.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Course implements Serializable {
    private static final long serialVersionUID = 1L;

    private String courseId;
    private String subjectCode;
    private String courseName;
    private String lecturerName;

    private int dayOfWeek;    // 2 (Mon) to 8 (Sun)
    private int startPeriod;  // 1 to 10
    private int endPeriod;    // startPeriod to 10

    private String room;
    private int maxSlots;
    private int remainingSlots;
    private LocalDateTime createdAt;

    public Course() {
    }

    public Course(String courseId, String subjectCode, String courseName, String lecturerName,
                  int dayOfWeek, int startPeriod, int endPeriod, String room,
                  int maxSlots, int remainingSlots, LocalDateTime createdAt) {
        this.courseId = courseId;
        this.subjectCode = subjectCode;
        this.courseName = courseName;
        this.lecturerName = lecturerName;
        this.dayOfWeek = dayOfWeek;
        this.startPeriod = startPeriod;
        this.endPeriod = endPeriod;
        this.room = room;
        this.maxSlots = maxSlots;
        this.remainingSlots = remainingSlots;
        this.createdAt = createdAt;
    }

    /**
     * Checks if this course conflicts in time with another course.
     * Overlapping formula: same day_of_week AND new.start <= existing.end AND new.end >= existing.start.
     */
    public boolean conflictsWith(Course other) {
        if (other == null) {
            return false;
        }
        if (this.dayOfWeek != other.dayOfWeek) {
            return false;
        }
        return this.startPeriod <= other.endPeriod && this.endPeriod >= other.startPeriod;
    }

    public boolean isFull() {
        return remainingSlots <= 0;
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

    public int getMaxSlots() {
        return maxSlots;
    }

    public void setMaxSlots(int maxSlots) {
        this.maxSlots = maxSlots;
    }

    public int getRemainingSlots() {
        return remainingSlots;
    }

    public void setRemainingSlots(int remainingSlots) {
        this.remainingSlots = remainingSlots;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Course{" +
                "courseId='" + courseId + '\'' +
                ", subjectCode='" + subjectCode + '\'' +
                ", courseName='" + courseName + '\'' +
                ", day=" + dayOfWeek +
                ", periods=" + startPeriod + "-" + endPeriod +
                ", remaining=" + remainingSlots + "/" + maxSlots +
                '}';
    }
}
