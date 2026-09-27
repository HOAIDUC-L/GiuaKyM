package com.course.client.state;

import com.course.common.dto.EnrollmentDetailDto;
import com.course.common.dto.LoginResponseDto;
import com.course.common.model.Course;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Stores local application state for the Client.
 */
public class ClientState {
    private static final ClientState INSTANCE = new ClientState();

    private LoginResponseDto currentUser;
    private final List<Course> cachedCourses = new CopyOnWriteArrayList<>();
    private final List<EnrollmentDetailDto> cachedEnrollments = new CopyOnWriteArrayList<>();

    public interface StateChangeListener {
        void onUserLoggedIn(LoginResponseDto user);
        void onUserLoggedOut();
        void onCoursesUpdated(List<Course> courses);
        void onEnrollmentsUpdated(List<EnrollmentDetailDto> enrollments);
        void onSingleCourseSlotUpdated(String courseId, int remainingSlots);
    }

    private final List<StateChangeListener> listeners = new CopyOnWriteArrayList<>();

    private ClientState() {
    }

    public static ClientState getInstance() {
        return INSTANCE;
    }

    public void addListener(StateChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(StateChangeListener listener) {
        listeners.remove(listener);
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public LoginResponseDto getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(LoginResponseDto user) {
        this.currentUser = user;
        if (user != null) {
            for (StateChangeListener l : listeners) {
                l.onUserLoggedIn(user);
            }
        } else {
            for (StateChangeListener l : listeners) {
                l.onUserLoggedOut();
            }
        }
    }

    public void clear() {
        this.currentUser = null;
        this.cachedCourses.clear();
        this.cachedEnrollments.clear();
        for (StateChangeListener l : listeners) {
            l.onUserLoggedOut();
        }
    }

    public List<Course> getCachedCourses() {
        return new ArrayList<>(cachedCourses);
    }

    public void setCachedCourses(List<Course> courses) {
        this.cachedCourses.clear();
        if (courses != null) {
            this.cachedCourses.addAll(courses);
        }
        for (StateChangeListener l : listeners) {
            l.onCoursesUpdated(this.cachedCourses);
        }
    }

    public void updateCourseSlot(String courseId, int remainingSlots) {
        for (Course c : cachedCourses) {
            if (c.getCourseId().equals(courseId)) {
                c.setRemainingSlots(remainingSlots);
                break;
            }
        }
        for (StateChangeListener l : listeners) {
            l.onSingleCourseSlotUpdated(courseId, remainingSlots);
        }
    }

    public List<EnrollmentDetailDto> getCachedEnrollments() {
        return new ArrayList<>(cachedEnrollments);
    }

    public void setCachedEnrollments(List<EnrollmentDetailDto> enrollments) {
        this.cachedEnrollments.clear();
        if (enrollments != null) {
            this.cachedEnrollments.addAll(enrollments);
        }
        for (StateChangeListener l : listeners) {
            l.onEnrollmentsUpdated(this.cachedEnrollments);
        }
    }
}
