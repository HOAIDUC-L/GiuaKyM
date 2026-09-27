# CƠ SỞ DỮ LIỆU POSTGRESQL (DATABASE DESIGN & DICTIONARY)

## 1. TỔNG QUAN

Hệ thống sử dụng cơ sở dữ liệu quan hệ **PostgreSQL 14+**. Cơ sở dữ liệu là nguồn dữ liệu chân thực duy nhất (Single Source of Truth), quản lý trạng thái tài khoản, danh mục lớp học phần, các bản ghi đăng ký và nhật ký phiên kết nối.

---

## 2. BẢNG DỮ LIỆU (TABLE SCHEMAS)

### 2.1. Bảng `users` (Người dùng)
Lưu thông tin tài khoản của Sinh viên và Quản trị viên.

| Tên cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
| :--- | :--- | :--- | :--- |
| `student_id` | VARCHAR(20) | PRIMARY KEY | Mã sinh viên hoặc mã định danh Admin |
| `email` | VARCHAR(100) | UNIQUE, NOT NULL | Địa chỉ email người dùng |
| `username` | VARCHAR(50) | UNIQUE, NOT NULL | Tên tài khoản đăng nhập |
| `password` | VARCHAR(255) | NOT NULL | Mật khẩu băm bằng BCrypt (Cost 12) |
| `full_name` | VARCHAR(100) | NOT NULL | Họ và tên đầy đủ |
| `class_name` | VARCHAR(50) | NULL | Tên lớp sinh hoạt (đối với sinh viên) |
| `role` | VARCHAR(20) | NOT NULL, DEFAULT 'STUDENT' | Quyền: `STUDENT` hoặc `ADMIN` |
| `is_active` | BOOLEAN | NOT NULL, DEFAULT TRUE | Trạng thái hoạt động (khóa/mở) |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Thời điểm tạo tài khoản |

* **Ràng buộc kiểm tra**:
  ```sql
  CONSTRAINT chk_users_role CHECK (role IN ('STUDENT', 'ADMIN'))
  ```

---

### 2.2. Bảng `courses` (Lớp học phần)
Lưu thông tin lịch học, giảng viên, phòng học và số lượng slot đăng ký.

| Tên cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
| :--- | :--- | :--- | :--- |
| `course_id` | VARCHAR(30) | PRIMARY KEY | Mã lớp học phần (ví dụ: `INT101_01`) |
| `subject_code` | VARCHAR(20) | NOT NULL | Mã môn học (ví dụ: `INT101`) |
| `course_name` | VARCHAR(150) | NOT NULL | Tên học phần |
| `lecturer_name` | VARCHAR(100) | NOT NULL | Giảng viên giảng dạy |
| `day_of_week` | INT | NOT NULL | Thứ trong tuần: 2 (Thứ 2) đến 8 (Chủ Nhật) |
| `start_period` | INT | NOT NULL | Tiết học bắt đầu (1 đến 10) |
| `end_period` | INT | NOT NULL | Tiết học kết thúc (`start_period` đến 10) |
| `room` | VARCHAR(50) | NULL | Phòng học |
| `max_slots` | INT | NOT NULL | Tổng số slot tối đa của lớp (> 0) |
| `remaining_slots` | INT | NOT NULL | Số slot còn lại |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Thời điểm tạo lớp học phần |

* **Ràng buộc kiểm tra (Constraints)**:
  ```sql
  CONSTRAINT chk_courses_day_of_week CHECK (day_of_week BETWEEN 2 AND 8);
  CONSTRAINT chk_courses_start_period CHECK (start_period BETWEEN 1 AND 10);
  CONSTRAINT chk_courses_end_period CHECK (end_period BETWEEN start_period AND 10);
  CONSTRAINT chk_courses_max_slots CHECK (max_slots > 0);
  CONSTRAINT chk_courses_remaining_slots CHECK (remaining_slots >= 0 AND remaining_slots <= max_slots);
  ```

---

### 2.3. Bảng `enrollments` (Đăng ký học phần)
Lưu vết sinh viên đăng ký thành công lớp học phần.

| Tên cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
| :--- | :--- | :--- | :--- |
| `enrollment_id` | BIGINT | PRIMARY KEY, IDENTITY | Khóa chính tự tăng |
| `student_id` | VARCHAR(20) | NOT NULL, FK $\rightarrow$ `users` | Khóa ngoại tới người dùng (`ON DELETE RESTRICT`) |
| `course_id` | VARCHAR(30) | NOT NULL, FK $\rightarrow$ `courses` | Khóa ngoại tới lớp học phần (`ON DELETE RESTRICT`) |
| `registered_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Thời điểm đăng ký thành công |

* **Ràng buộc duy nhất**:
  ```sql
  CONSTRAINT uq_student_course UNIQUE (student_id, course_id)
  ```
  *(Đảm bảo một sinh viên không thể đăng ký trùng 1 lớp học phần nhiều lần).*

---

### 2.4. Bảng `session_logs` (Nhật ký kết nối)
Kiểm toán các phiên kết nối và đăng nhập.

| Tên cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
| :--- | :--- | :--- | :--- |
| `session_id` | BIGINT | PRIMARY KEY, IDENTITY | Khóa chính tự tăng |
| `student_id` | VARCHAR(20) | NULL, FK $\rightarrow$ `users` | Sinh viên đăng nhập (`ON DELETE SET NULL`) |
| `client_ip` | VARCHAR(45) | NULL | Địa chỉ IP của Client |
| `client_port` | INT | NULL | Cổng mạng của Client |
| `login_time` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Thời điểm đăng nhập |
| `logout_time` | TIMESTAMP | NULL | Thời điểm đăng xuất / ngắt kết nối |
| `status` | VARCHAR(20) | NOT NULL | Trạng thái: `ONLINE`, `LOGOUT`, `KICKED`, `DISCONNECTED` |

* **Ràng buộc kiểm tra**:
  ```sql
  CONSTRAINT chk_session_status CHECK (status IN ('ONLINE', 'LOGOUT', 'KICKED', 'DISCONNECTED'))
  ```

---

## 3. CHỈ MỤC TỐI ƯU HIỆU NĂNG (INDEXES)

```sql
-- Tăng tốc truy vấn danh sách học phần của sinh viên
CREATE INDEX idx_enrollments_student ON enrollments(student_id);

-- Tăng tốc đếm và kiểm tra đăng ký của lớp học phần
CREATE INDEX idx_enrollments_course ON enrollments(course_id);

-- Tăng tốc tìm kiếm theo mã môn học và kiểm tra trùng mã môn
CREATE INDEX idx_courses_subject ON courses(subject_code);

-- Tối ưu hóa kiểm tra xung đột thời khóa biểu
CREATE INDEX idx_courses_schedule ON courses(day_of_week, start_period, end_period);

-- Tăng tốc tra cứu lịch sử phiên theo sinh viên
CREATE INDEX idx_session_logs_student ON session_logs(student_id);
```

---

## 4. DỮ LIỆU KIỂM THỬ MẪU (SEED DATA)

1. **Sinh viên**:
   - `sv01` / `123456` (Nguyễn Văn A, Lớp D21CQCN01-B, MSSV: `SV001`)
   - `sv02` / `123456` (Trần Thị B, Lớp D21CQCN02-B, MSSV: `SV002`)
   - `sv03` / `123456` (Lê Văn C, Lớp D21CQCN01-B, MSSV: `SV003`)
   - `sv_locked` / `123456` (`is_active = FALSE`, dùng để test tài khoản bị vô hiệu hóa)
2. **Quản trị viên**:
   - `admin` / `admin123` (Administrator, Quyền: `ADMIN`, MSSV: `ADMIN01`)
3. **Các kịch bản môn học**:
   - `INT101_01` (Thứ 2, tiết 1-3) & `INT101_02` (Thứ 4, tiết 6-8): Cùng mã môn `INT101`, khác lịch $\rightarrow$ kiểm tra phát hiện trùng mã môn.
   - `INT102_01` (Thứ 2, tiết 2-4): Trùng lịch tiết 2-3 với `INT101_01` $\rightarrow$ kiểm tra phát hiện trùng thời khóa biểu.
   - `COURSE_RACE_01`: Chỉ còn **đúng 1 slot** (`remaining_slots = 1`) $\rightarrow$ kiểm tra Race Condition giữa 2 hoặc nhiều Client đồng thời.
   - `INT107_01`: Đã hết chỗ (`remaining_slots = 0`) $\rightarrow$ kiểm tra từ chối `COURSE_FULL`.
