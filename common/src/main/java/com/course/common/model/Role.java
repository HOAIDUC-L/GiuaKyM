package com.course.common.model;

/**
 * User roles in the system.
 */
public enum Role {
    STUDENT,
    ADMIN;

    public static Role fromString(String roleStr) {
        if (roleStr == null) {
            return STUDENT;
        }
        try {
            return Role.valueOf(roleStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return STUDENT;
        }
    }
}
