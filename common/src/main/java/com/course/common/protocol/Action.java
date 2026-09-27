package com.course.common.protocol;

/**
 * Protocol actions supported by the Client-Server TCP socket communication.
 */
public enum Action {
    // Authentication
    LOGIN,
    LOGIN_RESPONSE,
    LOGOUT,
    LOGOUT_RESPONSE,

    // Heartbeat
    PING,
    PONG,

    // Student Course Queries & Actions
    GET_COURSES,
    GET_COURSES_RESPONSE,
    SEARCH_COURSES,
    SEARCH_COURSES_RESPONSE,
    GET_MY_ENROLLMENTS,
    GET_MY_ENROLLMENTS_RESPONSE,
    REGISTER_COURSE,
    REGISTER_COURSE_RESPONSE,
    CANCEL_COURSE,
    CANCEL_COURSE_RESPONSE,

    // Real-time Broadcast & Server Push
    COURSE_UPDATED,
    ENROLLMENT_UPDATED,
    FORCE_LOGOUT,

    // Admin Actions
    GET_ONLINE_CLIENTS,
    GET_ONLINE_CLIENTS_RESPONSE,
    KICK_CLIENT,
    KICK_CLIENT_RESPONSE,
    BLOCK_USER,
    BLOCK_USER_RESPONSE,
    UNBLOCK_USER,
    UNBLOCK_USER_RESPONSE,
    SERVER_STATUS,
    SERVER_STATUS_RESPONSE,

    // Generic Error
    ERROR;

    public static Action fromString(String actionStr) {
        if (actionStr == null) {
            return ERROR;
        }
        try {
            return Action.valueOf(actionStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ERROR;
        }
    }
}
