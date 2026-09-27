package com.course.common.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CourseConflictTest {

    private Course createCourse(String id, String subject, int day, int start, int end, int remaining) {
        return new Course(id, subject, "Test Course " + id, "Lecturer", day, start, end, "A101", 40, remaining, LocalDateTime.now());
    }

    @Test
    @DisplayName("Overlapping periods on same day should detect conflict")
    void testScheduleConflictOverlap() {
        // Mon (Day 2), Periods 1 to 3
        Course courseA = createCourse("C1", "INT101", 2, 1, 3, 40);
        // Mon (Day 2), Periods 2 to 4 (Overlaps at periods 2 and 3)
        Course courseB = createCourse("C2", "INT102", 2, 2, 4, 40);

        assertTrue(courseA.conflictsWith(courseB), "Should detect conflict when periods overlap");
        assertTrue(courseB.conflictsWith(courseA), "Conflict check must be symmetric");
    }

    @Test
    @DisplayName("Touching periods on same day should detect conflict (e.g. 1-3 and 3-5 overlap at period 3)")
    void testScheduleConflictTouchingBoundary() {
        Course courseA = createCourse("C1", "INT101", 2, 1, 3, 40);
        Course courseB = createCourse("C2", "INT102", 2, 3, 5, 40);

        assertTrue(courseA.conflictsWith(courseB), "Period 3 is shared, so they conflict");
    }

    @Test
    @DisplayName("Non-overlapping contiguous periods on same day should NOT conflict (e.g. 1-3 and 4-6)")
    void testScheduleNoConflictContiguous() {
        Course courseA = createCourse("C1", "INT101", 2, 1, 3, 40);
        Course courseB = createCourse("C2", "INT102", 2, 4, 6, 40);

        assertFalse(courseA.conflictsWith(courseB), "Periods 1-3 and 4-6 do not overlap");
    }

    @Test
    @DisplayName("Courses on different days should NEVER conflict even with identical periods")
    void testScheduleNoConflictDifferentDays() {
        // Monday (Day 2) Periods 1-3 vs Wednesday (Day 4) Periods 1-3
        Course courseA = createCourse("C1", "INT101", 2, 1, 3, 40);
        Course courseB = createCourse("C2", "INT102", 4, 1, 3, 40);

        assertFalse(courseA.conflictsWith(courseB), "Different days cannot conflict");
    }

    @Test
    @DisplayName("Course isFull should correctly detect when remaining slots <= 0")
    void testIsFull() {
        Course fullCourse = createCourse("C1", "INT101", 2, 1, 3, 0);
        Course availableCourse = createCourse("C2", "INT102", 2, 1, 3, 5);

        assertTrue(fullCourse.isFull());
        assertFalse(availableCourse.isFull());
    }
}
