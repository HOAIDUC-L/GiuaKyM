-- =============================================================================
-- COURSE REGISTRATION SYSTEM - DATABASE SCHEMA
-- PostgreSQL 14+
-- =============================================================================

-- Drop tables if exists (clean setup order honoring foreign keys)
DROP TABLE IF EXISTS session_logs CASCADE;
DROP TABLE IF EXISTS enrollments CASCADE;
DROP TABLE IF EXISTS courses CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- -----------------------------------------------------------------------------
-- 1. Table: users
-- Stores both students and administrators with BCrypt-hashed passwords.
-- -----------------------------------------------------------------------------
CREATE TABLE users (
    student_id  VARCHAR(20)  PRIMARY KEY,
    email       VARCHAR(100) UNIQUE NOT NULL,
    username    VARCHAR(50)  UNIQUE NOT NULL,
    password    VARCHAR(255) NOT NULL,
    full_name   VARCHAR(100) NOT NULL,
    class_name  VARCHAR(50),
    role        VARCHAR(20)  NOT NULL DEFAULT 'STUDENT',
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_users_role CHECK (role IN ('STUDENT', 'ADMIN'))
);

-- -----------------------------------------------------------------------------
-- 2. Table: courses
-- Stores course offerings, schedules, rooms, and slot availability.
-- Day of week: 2 = Monday, 3 = Tuesday, ..., 7 = Saturday, 8 = Sunday.
-- Periods: 1 to 10 (morning: 1-5, afternoon: 6-10).
-- -----------------------------------------------------------------------------
CREATE TABLE courses (
    course_id       VARCHAR(30)  PRIMARY KEY,
    subject_code    VARCHAR(20)  NOT NULL,
    course_name     VARCHAR(150) NOT NULL,
    lecturer_name   VARCHAR(100) NOT NULL,

    day_of_week     INT          NOT NULL,
    start_period    INT          NOT NULL,
    end_period      INT          NOT NULL,

    room            VARCHAR(50),

    max_slots       INT          NOT NULL,
    remaining_slots INT          NOT NULL,

    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_courses_day_of_week CHECK (day_of_week BETWEEN 2 AND 8),
    CONSTRAINT chk_courses_start_period CHECK (start_period BETWEEN 1 AND 10),
    CONSTRAINT chk_courses_end_period CHECK (end_period BETWEEN start_period AND 10),
    CONSTRAINT chk_courses_max_slots CHECK (max_slots > 0),
    CONSTRAINT chk_courses_remaining_slots CHECK (remaining_slots >= 0 AND remaining_slots <= max_slots)
);

-- -----------------------------------------------------------------------------
-- 3. Table: enrollments
-- Records of course registrations.
-- RESTRICT on delete prevents orphan enrollments if users/courses are deleted.
-- UNIQUE(student_id, course_id) prevents registering the same course twice.
-- -----------------------------------------------------------------------------
CREATE TABLE enrollments (
    enrollment_id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    student_id      VARCHAR(20) NOT NULL REFERENCES users(student_id) ON DELETE RESTRICT,
    course_id       VARCHAR(30) NOT NULL REFERENCES courses(course_id) ON DELETE RESTRICT,
    registered_at   TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_student_course UNIQUE (student_id, course_id)
);

-- -----------------------------------------------------------------------------
-- 4. Table: session_logs
-- Audit log of client connections and authentication sessions.
-- -----------------------------------------------------------------------------
CREATE TABLE session_logs (
    session_id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    student_id      VARCHAR(20) REFERENCES users(student_id) ON DELETE SET NULL,
    client_ip       VARCHAR(45),
    client_port     INT,
    login_time      TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    logout_time     TIMESTAMP,
    status          VARCHAR(20) NOT NULL DEFAULT 'ONLINE',

    CONSTRAINT chk_session_status CHECK (status IN ('ONLINE', 'LOGOUT', 'KICKED', 'DISCONNECTED'))
);

-- -----------------------------------------------------------------------------
-- 5. Performance Indexes
-- -----------------------------------------------------------------------------
CREATE INDEX idx_enrollments_student  ON enrollments(student_id);
CREATE INDEX idx_enrollments_course   ON enrollments(course_id);
CREATE INDEX idx_courses_subject      ON courses(subject_code);
CREATE INDEX idx_courses_schedule     ON courses(day_of_week, start_period, end_period);
CREATE INDEX idx_session_logs_student ON session_logs(student_id);
