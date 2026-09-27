package com.course.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CancelCourseRequestDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String courseId;

    public CancelCourseRequestDto() {
    }

    public CancelCourseRequestDto(String courseId) {
        this.courseId = courseId;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }
}
