-- =============================================================================
-- COURSE REGISTRATION SYSTEM - TEST DATA & SCENARIO FIXTURES
-- Fixtures for automated and manual test scenarios
-- =============================================================================

-- 1. Disabled student account (for testing ACCOUNT_DISABLED on login)
INSERT INTO users (student_id, email, username, password, full_name, class_name, role, is_active)
VALUES
    ('SV_LOCKED', 'locked@student.ptit.edu.vn', 'sv_locked', '$2a$12$7Bu8DxKorw8zQ.xP0QvmF.OGfRrybZw.UADjJgwbWNV0ej.pKyz26', 'Sinh Viên Bị Khóa', 'D21CQCN03-B', 'STUDENT', FALSE)
ON CONFLICT (student_id) DO UPDATE SET is_active = FALSE;

-- 2. Additional Race Condition Course Fixture (1 slot left)
INSERT INTO courses (course_id, subject_code, course_name, lecturer_name, day_of_week, start_period, end_period, room, max_slots, remaining_slots)
VALUES
    ('COURSE_RACE_02', 'INT109', 'Học Máy Nâng Cao (Special)', 'GS. TS. Lê Hồng P', 4, 1, 3, 'LAB-AI', 20, 1)
ON CONFLICT (course_id) DO UPDATE SET remaining_slots = 1;

-- 3. Reset helper function / statements to restore race course slots
-- Can be called between test runs:
-- UPDATE courses SET remaining_slots = 1 WHERE course_id IN ('COURSE_RACE_01', 'COURSE_RACE_02');
-- DELETE FROM enrollments WHERE course_id IN ('COURSE_RACE_01', 'COURSE_RACE_02');
