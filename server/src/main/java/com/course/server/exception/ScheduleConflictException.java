package com.course.server.exception;

import com.course.common.protocol.ErrorCode;

public class ScheduleConflictException extends CourseRegistrationException {
    public ScheduleConflictException(String message) {
        super(ErrorCode.SCHEDULE_CONFLICT, message);
    }
}
