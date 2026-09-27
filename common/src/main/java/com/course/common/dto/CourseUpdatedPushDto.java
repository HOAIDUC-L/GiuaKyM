package com.course.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CourseUpdatedPushDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String courseId;
    private int remainingSlots;

    public CourseUpdatedPushDto() {
    }

    public CourseUpdatedPushDto(String courseId, int remainingSlots) {
        this.courseId = courseId;
        this.remainingSlots = remainingSlots;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public int getRemainingSlots() {
        return remainingSlots;
    }

    public void setRemainingSlots(int remainingSlots) {
        this.remainingSlots = remainingSlots;
    }
}
