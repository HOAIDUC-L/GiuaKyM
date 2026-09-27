-- =============================================================================
-- COURSE REGISTRATION SYSTEM - SEED DATA
-- Default accounts and course catalog
-- =============================================================================

-- Clear existing data
TRUNCATE TABLE session_logs RESTART IDENTITY CASCADE;
TRUNCATE TABLE enrollments RESTART IDENTITY CASCADE;
DELETE FROM courses;
DELETE FROM users;

-- -----------------------------------------------------------------------------
-- 1. Users Seed Data
-- Passwords are encrypted with BCrypt (Cost 12):
--   - '123456'   -> $2a$12$7Bu8DxKorw8zQ.xP0QvmF.OGfRrybZw.UADjJgwbWNV0ej.pKyz26
--   - 'admin123' -> $2a$12$QAPC1dxlMLegcEOhi0V4E.Kurj80xZOKsmSDtxu0grsDBW8eEn5pa
-- -----------------------------------------------------------------------------
INSERT INTO users (student_id, email, username, password, full_name, class_name, role, is_active)
VALUES
    ('SV001', 'sv01@student.ptit.edu.vn', 'sv01', '$2a$12$7Bu8DxKorw8zQ.xP0QvmF.OGfRrybZw.UADjJgwbWNV0ej.pKyz26', 'Nguyễn Văn A', 'D21CQCN01-B', 'STUDENT', TRUE),
    ('SV002', 'sv02@student.ptit.edu.vn', 'sv02', '$2a$12$7Bu8DxKorw8zQ.xP0QvmF.OGfRrybZw.UADjJgwbWNV0ej.pKyz26', 'Trần Thị B', 'D21CQCN02-B', 'STUDENT', TRUE),
    ('SV003', 'sv03@student.ptit.edu.vn', 'sv03', '$2a$12$7Bu8DxKorw8zQ.xP0QvmF.OGfRrybZw.UADjJgwbWNV0ej.pKyz26', 'Lê Văn C', 'D21CQCN01-B', 'STUDENT', TRUE),
    ('ADMIN01', 'admin@admin.ptit.edu.vn', 'admin', '$2a$12$QAPC1dxlMLegcEOhi0V4E.Kurj80xZOKsmSDtxu0grsDBW8eEn5pa', 'Quản Trị Viên Hệ Thống', NULL, 'ADMIN', TRUE);

-- -----------------------------------------------------------------------------
-- 2. Courses Seed Data
-- Schedule rules:
--   day_of_week: 2 (Mon), 3 (Tue), 4 (Wed), 5 (Thu), 6 (Fri), 7 (Sat), 8 (Sun)
--   start_period, end_period: 1 to 10
--
-- Test Case 1: Same subject_code (INT101), different lecturers, different schedules
--   - INT101_01: Mon (Day 2), P1-P3, TS. Nguyễn Văn D
--   - INT101_02: Wed (Day 4), P6-P8, PGS. TS. Trần Văn E
--
-- Test Case 2: Schedule conflict on Mon (Day 2)
--   - INT101_01: Day 2, P1-P3
--   - INT102_01: Day 2, P2-P4 (Overlaps P2-P3)
--
-- Test Case 3: Race condition test (remaining_slots = 1)
--   - COURSE_RACE_01: Day 5, P1-P3, max 30, remaining 1
--
-- Test Case 4: Course full test (remaining_slots = 0)
--   - INT107_01: Day 6, P6-P8, max 30, remaining 0
-- -----------------------------------------------------------------------------
INSERT INTO courses (course_id, subject_code, course_name, lecturer_name, day_of_week, start_period, end_period, room, max_slots, remaining_slots)
VALUES
    -- Case 1: Same subject_code = INT101, different sections
    ('INT101_01', 'INT101', 'Lập trình Mạng', 'TS. Nguyễn Văn D', 2, 1, 3, '301-A2', 40, 40),
    ('INT101_02', 'INT101', 'Lập trình Mạng', 'PGS. TS. Trần Văn E', 4, 6, 8, '405-A3', 40, 40),

    -- Case 2: Schedule conflict with INT101_01 (Mon, P2-P4 overlaps P1-P3)
    ('INT102_01', 'INT102', 'Cấu trúc Dữ liệu & Giải thuật', 'TS. Hoàng Thị F', 2, 2, 4, '202-A2', 35, 35),

    -- Case 3: Race condition test course (Exactly 1 slot remaining)
    ('COURSE_RACE_01', 'INT103', 'An toàn Hệ thống Thông tin (CLC)', 'ThS. Vũ Văn G', 5, 1, 3, 'LAB-02', 30, 1),

    -- Case 4: Full course (0 slots remaining)
    ('INT107_01', 'INT107', 'Điện toán Đám mây', 'TS. Đinh Văn M', 6, 6, 8, 'LAB-04', 30, 0),

    -- Other courses for rich timetable presentation
    ('INT104_01', 'INT104', 'Cơ sở Dữ liệu Phân tán', 'TS. Đỗ Mạnh H', 3, 1, 3, '101-A1', 45, 45),
    ('INT105_01', 'INT105', 'Phát triển Ứng dụng Web', 'ThS. Phạm Quang K', 3, 6, 8, '204-A3', 40, 40),
    ('INT106_01', 'INT106', 'Trí tuệ Nhân tạo', 'PGS. TS. Bùi Thị L', 6, 1, 3, '501-A2', 50, 50),
    ('INT108_01', 'INT108', 'Kỹ thuật Phần mềm', 'TS. Ngô Quốc N', 7, 2, 4, '302-A2', 40, 40);
