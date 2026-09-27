package com.course.server.service;

import com.course.common.dto.RegisterCourseRequestDto;
import com.course.common.protocol.Action;
import com.course.common.protocol.ErrorCode;
import com.course.common.protocol.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Concurrency test simulating multiple clients competing simultaneously
 * for the last remaining slot of a course (remaining_slots = 1).
 * Verifies transaction lock semantics and prevents overbooking / negative slots.
 */
class ConcurrencyRaceConditionTest {

    /**
     * Model simulating the database row-level locking (SELECT ... FOR UPDATE) and transaction semantics.
     */
    static class SimulatedDatabaseCourse {
        private final String courseId;
        private int remainingSlots;
        private final List<String> enrollments = new ArrayList<>();
        private final ReentrantLock rowLock = new ReentrantLock(); // Simulates SELECT ... FOR UPDATE

        public SimulatedDatabaseCourse(String courseId, int initialSlots) {
            this.courseId = courseId;
            this.remainingSlots = initialSlots;
        }

        public Response register(String studentId, String reqId) {
            // Emulate BEGIN TRANSACTION + SELECT ... FOR UPDATE
            rowLock.lock();
            try {
                // Simulate processing latency inside transaction
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {
                }

                if (enrollments.contains(studentId)) {
                    return Response.error(Action.REGISTER_COURSE_RESPONSE, reqId, ErrorCode.ALREADY_ENROLLED, "Đã đăng ký trước đó");
                }

                // Check slot availability
                if (remainingSlots <= 0) {
                    return Response.error(Action.REGISTER_COURSE_RESPONSE, reqId, ErrorCode.COURSE_FULL, "Lớp đã hết chỗ");
                }

                // Deduct slot & record enrollment (ATOMIC IN TRANSACTION)
                remainingSlots--;
                enrollments.add(studentId);

                // COMMIT TRANSACTION
                return Response.success(Action.REGISTER_COURSE_RESPONSE, reqId, "Đăng ký thành công", remainingSlots);
            } finally {
                // Release lock on COMMIT/ROLLBACK
                rowLock.unlock();
            }
        }

        public int getRemainingSlots() {
            return remainingSlots;
        }

        public int getEnrollmentCount() {
            return enrollments.size();
        }
    }

    @Test
    @DisplayName("Race condition test: 10 concurrent clients compete for 1 final slot. Exactly 1 succeeds, 9 receive COURSE_FULL")
    void testLastSlotRaceCondition() throws InterruptedException {
        int numberOfClients = 10;
        SimulatedDatabaseCourse course = new SimulatedDatabaseCourse("COURSE_RACE_01", 1);

        ExecutorService executor = Executors.newFixedThreadPool(numberOfClients);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(numberOfClients);

        List<Response> results = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger courseFullCount = new AtomicInteger(0);

        for (int i = 1; i <= numberOfClients; i++) {
            final String studentId = "SV00" + i;
            final String reqId = "REQ-" + i;
            executor.submit(() -> {
                try {
                    // Wait at starting gate so all threads hit register simultaneously
                    startGate.await();
                    Response res = course.register(studentId, reqId);
                    results.add(res);

                    if (res.isSuccess()) {
                        successCount.incrementAndGet();
                    } else if (res.getErrorCode() == ErrorCode.COURSE_FULL) {
                        courseFullCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    endGate.countDown();
                }
            });
        }

        // Release all threads simultaneously
        startGate.countDown();
        boolean finished = endGate.await(10, TimeUnit.SECONDS);
        assertTrue(finished, "All requests should finish within timeout");
        executor.shutdown();

        // Verification assertions according to Master Specification Section 20 & 48:
        assertEquals(1, successCount.get(), "EXACTLY 1 client must succeed in registering the last slot");
        assertEquals(numberOfClients - 1, courseFullCount.get(), "All other clients must receive COURSE_FULL");
        assertEquals(0, course.getRemainingSlots(), "Remaining slots must be exactly 0, never negative (-1)");
        assertEquals(1, course.getEnrollmentCount(), "Total enrollments must increase by exactly 1");
    }
}
