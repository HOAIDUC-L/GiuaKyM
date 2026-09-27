# KỊCH BẢN KIỂM THỬ HỆ THỐNG (TEST SCENARIOS & PROCEDURES)

Tài liệu này hướng dẫn chi tiết các bước kiểm thử chức năng và phi chức năng cho hệ thống Đăng ký học phần Client–Server.

---

## KỊCH BẢN 1: XÁC THỰC VÀ BẢO MẬT PHIÊN (AUTHENTICATION & SINGLE-SESSION)

### Bước 1.1: Đăng nhập thành công
* **Thao tác**: Nhập Username `sv01`, Password `123456`, nhấn `[ĐĂNG NHẬP]`.
* **Kỳ vọng**: Đăng nhập thành công, cửa sổ `LoginFrame` đóng lại, `MainFrame` xuất hiện hiển thị đầy đủ thông tin: Sinh viên Nguyễn Văn A, MSSV: SV001, Lớp D21CQCN01-B, Trạng thái: ● Trực tuyến.

### Bước 1.2: Đăng nhập sai mật khẩu
* **Thao tác**: Nhập Username `sv01`, Password `sai_mat_khau`.
* **Kỳ vọng**: Server từ chối, hiển thị thông báo lỗi `AUTH_FAILED: Tài khoản hoặc mật khẩu không chính xác`.

### Bước 1.3: Đăng nhập tài khoản bị khóa
* **Thao tác**: Nhập Username `sv_locked`, Password `123456`.
* **Kỳ vọng**: Server từ chối, hiển thị lỗi `ACCOUNT_DISABLED: Tài khoản của bạn đã bị vô hiệu hóa hoặc bị khóa bởi quản trị viên`.

### Bước 1.4: Chính sách Một phiên hoạt động duy nhất (Single-Session Policy)
* **Thao tác**:
  1. Khởi động Client 1, đăng nhập với `sv01` $\rightarrow$ Thành công.
  2. Khởi động Client 2, thử đăng nhập tiếp với `sv01`.
* **Kỳ vọng**: Client 2 bị từ chối với thông báo: `ALREADY_LOGGED_IN: Tài khoản này hiện đang được đăng nhập ở một thiết bị khác`. Client 1 vẫn hoạt động bình thường.

---

## KỊCH BẢN 2: PHÁT HIỆN TRÙNG LỊCH HỌC (SCHEDULE CONFLICT)

* **Chuẩn bị**: Sinh viên `sv01` đã đăng nhập.
* **Thao tác**:
  1. Chọn lớp `INT101_01` (Thứ 2, tiết 1–3, phòng 301-A2), nhấn `[ĐĂNG KÝ]`.
  2. Nhận thông báo đăng ký thành công. Lớp xuất hiện trong Tab "Thời Khóa Biểu Tuần" tại Thứ 2 (tiết 1, 2, 3).
  3. Tìm lớp `INT102_01` (Thứ 2, tiết 2–4, phòng 202-A2). Lớp này bị trùng vào tiết 2 và 3 của Thứ 2.
  4. Chọn `INT102_01` và nhấn `[ĐĂNG KÝ]`.
* **Kỳ vọng**:
  - Server thực hiện Transaction, kiểm tra conflict và tự động Rollback.
  - Client nhận phản hồi lỗi: `SCHEDULE_CONFLICT: Trùng lịch với môn 'Lập trình Mạng' (INT101_01) vào Thứ 2, tiết 1-3`.
  - Số slot của `INT102_01` không bị giảm. Không có bản ghi nào được thêm vào CSDL.

---

## KỊCH BẢN 3: PHÁT HIỆN TRÙNG MÃ MÔN HỌC (DUPLICATE SUBJECT CODE)

* **Chuẩn bị**: Sinh viên `sv01` đã đăng ký lớp `INT101_01` (Môn học: INT101 - Lập trình Mạng).
* **Thao tác**:
  1. Chọn lớp `INT101_02` (cũng có mã môn `INT101`, nhưng học vào Thứ 4 tiết 6–8, giảng viên khác, không trùng giờ).
  2. Nhấn `[ĐĂNG KÝ]`.
* **Kỳ vọng**:
  - Mặc dù hai lớp hoàn toàn khác thứ và khác tiết, Server kiểm tra ràng buộc nghiệp vụ môn học.
  - Server từ chối và trả về mã lỗi: `DUPLICATE_SUBJECT: Bạn đã đăng ký một lớp học phần khác thuộc môn học (INT101)`.

---

## KỊCH BẢN 4: REAL-TIME BROADCAST (CẬP NHẬT SLOT TỨC THỜI)

* **Chuẩn bị**: Khởi động 2 Client cùng lúc.
  - Client A: Đăng nhập tài khoản `sv01`.
  - Client B: Đăng nhập tài khoản `sv02`.
* **Thao tác**:
  1. Cả Client A và Client B đều mở Tab "Đăng Ký Học Phần", nhìn vào môn `INT104_01` (Slot còn lại hiện tại: 45).
  2. Phía Client A: Chọn `INT104_01` và bấm `[ĐĂNG KÝ HỌC PHẦN ĐÃ CHỌN]`.
  3. Client A nhận thông báo đăng ký thành công.
  4. Quan sát màn hình Client B **mà không hề bấm bất kỳ nút nào, không làm mới**.
* **Kỳ vọng**:
  - Ngay trong tích tắc, ô "Còn lại" của môn `INT104_01` trên bảng của Client B tự động nhảy từ `45` xuống `44`.
  - Server đã phát sóng gói tin push `COURSE_UPDATED` qua Dedicated Reader Thread của Client B.

---

## KỊCH BẢN 5: KIỂM THỬ RACE CONDITION (TRANH CHẤP SLOT CUỐI CÙNG)

* **Chuẩn bị**: Lớp học phần `COURSE_RACE_01` chỉ còn **đúng 1 slot duy nhất** (`remaining_slots = 1`).
* **Thao tác**:
  1. Client A (`sv01`) và Client B (`sv02`) cùng chọn môn `COURSE_RACE_01`.
  2. Cả 2 người dùng nhấn nút `[ĐĂNG KÝ]` gần như đồng thời (hoặc chạy qua test tự động `ConcurrencyRaceConditionTest`).
* **Kỳ vọng**:
  - Do Server sử dụng `SELECT ... FOR UPDATE` trong Transaction:
    - Đúng 1 Client nhận được thông báo: `Đăng ký học phần thành công`.
    - Client đến sau nhận được lỗi: `COURSE_FULL: Lớp học phần COURSE_RACE_01 đã hết chỗ đăng ký`.
  - Kiểm tra CSDL PostgreSQL:
    ```sql
    SELECT remaining_slots FROM courses WHERE course_id = 'COURSE_RACE_01'; -- Kết quả: 0 (tuyệt đối không bao giờ là -1)
    SELECT COUNT(*) FROM enrollments WHERE course_id = 'COURSE_RACE_01';    -- Kết quả: Đúng 1 bản ghi
    ```

---

## KỊCH BẢN 6: QUẢN TRỊ VIÊN ĐÁ CLIENT (ADMIN KICK CLIENT)

* **Thao tác**:
  1. Sinh viên `sv01` đang mở Client và thao tác bình thường.
  2. Quản trị viên mở `Admin Server Dashboard`, nhìn vào bảng "Online Clients" thấy dòng của `sv01` (IP, Port, Session ID).
  3. Admin chọn dòng `sv01` và nhấn nút `[KICK CLIENT ĐÃ CHỌN]`.
* **Kỳ vọng**:
  - Phía Client của `sv01`: Ngay lập tức nhận được hộp thoại cảnh báo: `"Bạn đã bị quản trị viên đá khỏi hệ thống."`.
  - Khi bấm OK: Cửa sổ `MainFrame` tự động đóng, session local bị xóa, kết nối socket bị ngắt và quay về `LoginFrame`.
  - Bảng Online Clients của Admin tự động mất dòng của `sv01`.

---

## KỊCH BẢN 7: KHÓA TÀI KHOẢN KHI ĐANG ONLINE (ADMIN BLOCK ACCOUNT)

* **Thao tác**:
  1. Sinh viên `sv02` đang đăng nhập và online.
  2. Trên Admin Dashboard, Admin nhập `sv02` vào ô tài khoản và bấm `[KHÓA TÀI KHOẢN (BLOCK)]`.
* **Kỳ vọng**:
  - CSDL cập nhật `users.is_active = FALSE`.
  - Server phát hiện tài khoản `sv02` đang online $\rightarrow$ Lập tức gửi lệnh `FORCE_LOGOUT` và đóng socket.
  - Sinh viên `sv02` bị đẩy văng ra màn hình đăng nhập.
  - Sinh viên cố tình nhập mật khẩu đăng nhập lại $\rightarrow$ Bị từ chối ngay với lỗi `ACCOUNT_DISABLED`.

---

## KỊCH BẢN 8: XUẤT THỜI KHÓA BIỂU RA EXCEL (APACHE POI)

* **Thao tác**:
  1. Sinh viên đăng ký ít nhất 2 môn học (ví dụ: `INT101_01`, `INT104_01`).
  2. Mở Tab "Học Phần Đã Đăng Ký", nhấn nút `[Xuất Thời Khóa Biểu Ra Excel]`.
  3. Chọn nơi lưu file: `schedule_SV001_2026-09-26.xlsx`.
* **Kỳ vọng**:
  - File Excel `.xlsx` được tạo hoàn chỉnh.
  - Mở file kiểm tra:
    - **Sheet 1 ("Thời Khóa Biểu")**: Ma trận 10 tiết $\times$ 7 ngày có kẻ khung, tiêu đề in đậm màu xanh, các ô có môn học được tô màu phân biệt hiển thị mã môn, tên môn, phòng, giảng viên.
    - **Sheet 2 ("Học Phần Đã Đăng Ký")**: Bảng chi tiết có STT, Mã Lớp HP, Mã Môn, Tên Học Phần, Giảng Viên, Lịch Học, Phòng Học, Ngày Đăng Ký. Các cột tự động căn chỉnh kích thước (Auto-size).
