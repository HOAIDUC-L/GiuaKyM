package com.course.server.exception;

import com.course.common.protocol.ErrorCode;

public class CourseNotFoundException extends CourseRegistrationException {
    public CourseNotFoundException(String message) {
        super(ErrorCode.COURSE_NOT_FOUND, message);
    }
}
