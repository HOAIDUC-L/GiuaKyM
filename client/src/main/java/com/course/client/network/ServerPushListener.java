package com.course.client.network;

/**
 * Listener interface for asynchronous server push events.
 */
public interface ServerPushListener {

    /**
     * Triggered when a course's remaining slots are updated in real-time.
     */
    void onCourseUpdated(String courseId, int remainingSlots);

    /**
     * Triggered when the server forces the client to logout (Admin kick / account blocked / server shutdown).
     */
    void onForceLogout(String message);

    /**
     * Triggered when the socket connection is lost.
     */
    void onDisconnected(String reason);

    /**
     * Triggered when the socket successfully connects to the server.
     */
    default void onConnected() {
    }
}
