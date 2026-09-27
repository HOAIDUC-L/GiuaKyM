# HỆ THỐNG ĐĂNG KÝ HỌC PHẦN CLIENT–SERVER (TCP SOCKET)

> Đồ án môn học: **Lập trình Mạng (Network Programming)**  
> Kiến trúc: **TCP Client–Server / Multi-threading / PostgreSQL JDBC / Swing UI / JSON Protocol / Apache POI**  
> **Tuyệt đối KHÔNG sử dụng Spring Boot, Hibernate, JPA hay RMI.**

---

## 1. TỔNG QUAN DỰ ÁN

Dự án triển khai một hệ thống đăng ký học phần đại học hoàn chỉnh theo mô hình Client–Server chuẩn:
* **Server**: Đóng vai trò là nguồn dữ liệu duy nhất (**Single Source of Truth**), quản lý các kết nối đa luồng qua TCP `ServerSocket`, xác thực và phân quyền, thực thi giao dịch CSDL với khóa dòng (`SELECT ... FOR UPDATE`), và phát sóng sự kiện thời gian thực (Real-time Broadcast).
* **Client (Sinh viên)**: Giao diện đồ họa Java Swing hiện đại, kết nối tới Server qua TCP `Socket`, trao đổi dữ liệu hoàn toàn bằng định dạng JSON (phân tách bởi ký tự xuống dòng `\n`). Có Dedicated Reader Thread nhận cập nhật slot tức thời từ Server. **Client tuyệt đối không kết nối trực tiếp đến PostgreSQL.**
* **Server Admin Dashboard**: Bảng điều khiển quản trị đồ họa cho phép Start/Stop máy chủ, giám sát danh sách Client đang kết nối thời gian thực, ngắt kết nối (Kick) và khóa/mở khóa tài khoản sinh viên.

---

## 2. CÁC TÍNH NĂNG CHÍNH

### Dành cho Sinh viên (Client GUI)
1. **Đăng nhập & Quản lý phiên**: Xác thực an toàn bằng mật khẩu mã hóa BCrypt, áp dụng chính sách một phiên duy nhất (**Single-Session Policy**) chống đăng nhập trùng lặp.
2. **Tra cứu & Tìm kiếm học phần**: Xem danh mục lớp học phần, tìm kiếm theo tên môn, mã môn, giảng viên.
3. **Đăng ký học phần thông minh**:
   - Kiểm tra và từ chối khi lớp đã hết slot (`COURSE_FULL`).
   - Tự động phát hiện và ngăn chặn đăng ký trùng môn học (`DUPLICATE_SUBJECT`).
   - Phát hiện xung đột thời khóa biểu giữa các môn học (`SCHEDULE_CONFLICT`).
4. **Cập nhật Slot Real-Time (Server Push / Broadcast)**: Khi bất kỳ sinh viên nào đăng ký hoặc hủy môn, số slot còn lại trên bảng của mọi sinh viên khác sẽ tự động cập nhật ngay lập tức mà không cần bấm làm mới.
5. **Xem Thời khóa biểu trực quan**: Ma trận 10 Tiết $\times$ 7 Ngày (Thứ 2 $\rightarrow$ Chủ Nhật), hiển thị trực quan các môn học đã đăng ký.
6. **Hủy đăng ký học phần**: Hoàn lại slot vào kho học phần, cập nhật thời khóa biểu và phát sóng broadcast.
7. **Xuất Thời khóa biểu ra Excel (.xlsx)**: Sử dụng Apache POI tạo file Excel 2 sheet (Sheet Ma trận Thời khóa biểu và Sheet Danh sách môn học) có kẻ khung, tô màu và auto-size cột.
8. **Nhận thông báo cưỡng bức (Force Logout)**: Tự động đăng xuất và thông báo khi bị Admin kick hoặc khóa tài khoản.

### Dành cho Quản trị viên (Server Admin Dashboard)
1. **Quản lý máy chủ**: Khởi động (Start) / Dừng (Stop) Server Socket, cấu hình cổng Port.
2. **Theo dõi Client Online**: Xem danh sách các máy trạm đang kết nối thời gian thực (Session ID, IP, Port, MSSV, Username, Họ tên, Thời gian login, Thời gian online).
3. **Đá Client (Kick Client)**: Ngắt kết nối socket của client được chọn và ép client về màn hình đăng nhập.
4. **Khóa / Mở khóa tài khoản (Block / Unblock)**: Khóa tài khoản sinh viên ngay lập tức; nếu sinh viên đang online sẽ bị ngắt kết nối ngay tức thì.
5. **Console Nhật ký (Event Log)**: Theo dõi toàn bộ sự kiện mạng, đăng nhập, giao dịch CSDL theo thời gian thực.

---

## 3. CÔNG NGHỆ & CẤU TRÚC DỰ ÁN

* **Ngôn ngữ**: Java 17+ (Java Core).
* **Quản lý dự án**: Maven Multi-Module.
* **Mạng (Network)**: `java.net.ServerSocket`, `java.net.Socket`, Thread Pool (`java.util.concurrent.ExecutorService`), Dedicated Reader Thread, Scheduled Heartbeat (`PING`/`PONG`).
* **Giao thức**: JSON UTF-8 framing bằng ký tự xuống dòng `\n` (`BufferedReader.readLine()` / `PrintWriter.println()`).
* **Cơ sở dữ liệu**: PostgreSQL 14+, JDBC thuần với `PreparedStatement`, Connection Pool `HikariCP`.
* **Kiểm soát đồng thời (Concurrency)**: `Connection.setAutoCommit(false)`, `SELECT ... FOR UPDATE` (Row-Level Locking), `commit()`, `rollback()`.
* **Thư viện bên ngoài**:
  - `jackson-databind`: Xử lý JSON.
  - `jbcrypt`: Băm và kiểm tra mật khẩu.
  - `poi` & `poi-ooxml`: Xuất báo cáo Excel.
  - `flatlaf`: Giao diện đồ họa Swing hiện đại.
  - `junit-jupiter`: Kiểm thử tự động.

### Cấu trúc thư mục (Multi-module Tree):
```text
course-registration-system/
├── pom.xml
├── common/                               # Module dùng chung
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/course/common/
│       │   ├── constants/AppConstants.java
│       │   ├── protocol/Action.java, ErrorCode.java, Request.java, Response.java
│       │   ├── model/User.java, Course.java, Enrollment.java, SessionLog.java, Role.java
│       │   ├── dto/...
│       │   └── util/JsonUtil.java
│       └── test/java/...
├── server/                               # Module Server & Admin GUI
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/course/server/
│       │   │   ├── ServerApplication.java, ServerContext.java
│       │   │   ├── config/ServerConfig.java
│       │   │   ├── network/TcpServer.java, ClientHandler.java, ProtocolHandler.java
│       │   │   ├── session/ClientSession.java, SessionManager.java
│       │   │   ├── database/DatabaseManager.java
│       │   │   ├── repository/UserRepository.java, CourseRepository.java, ...
│       │   │   ├── service/AuthService.java, EnrollmentService.java, BroadcastService.java, ...
│       │   │   ├── exception/...
│       │   │   └── ui/AdminDashboardFrame.java, ServerLogAppender.java
│       │   └── resources/application.properties, application-example.properties
│       └── test/java/...
├── client/                               # Module Client Swing GUI
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/course/client/
│       │   │   ├── ClientApplication.java
│       │   │   ├── config/ClientConfig.java
│       │   │   ├── network/ClientSocketManager.java, ServerPushListener.java
│       │   │   ├── state/ClientState.java
│       │   │   ├── service/ClientService.java, ScheduleExcelExporter.java
│       │   │   └── ui/LoginFrame.java, MainFrame.java, CoursePanel.java, ...
│       │   └── resources/application.properties
│       └── test/java/...
├── database/                             # Kịch bản CSDL PostgreSQL
│   ├── schema.sql
│   ├── seed.sql
│   └── test-data.sql
├── docs/                                 # Tài liệu kiến trúc & giao thức
│   ├── architecture.md
│   ├── protocol.md
│   ├── database.md
│   └── test-scenarios.md
└── README.md
```

---

## 4. TÀI KHOẢN MẪU (TEST ACCOUNTS)

| Loại tài khoản | Tên đăng nhập | Mật khẩu | MSSV | Họ tên | Lớp / Quyền |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Sinh viên** | `sv01` | `123456` | `SV001` | Nguyễn Văn A | D21CQCN01-B |
| **Sinh viên** | `sv02` | `123456` | `SV002` | Trần Thị B | D21CQCN02-B |
| **Sinh viên** | `sv03` | `123456` | `SV003` | Lê Văn C | D21CQCN01-B |
| **Bị khóa (Test)** | `sv_locked`| `123456` | `SV_LOCKED` | Sinh Viên Bị Khóa | Bị khóa (`is_active=false`) |
| **Quản trị viên** | `admin` | `admin123` | `ADMIN01` | Quản Trị Viên | `ADMIN` |

*(Toàn bộ mật khẩu trong `seed.sql` đã được băm chuẩn BCrypt Cost 12)*.

---

## 5. HƯỚNG DẪN CÀI ĐẶT & KHỞI CHẠY

### Yêu cầu tiên quyết:
- JDK 17 trở lên (`java -version`).
- Apache Maven 3.8+ (`mvn -version`).
- PostgreSQL 14+ đang chạy trên cổng 5432.

### Bước 1: Thiết lập Cơ sở dữ liệu PostgreSQL
Mở terminal và thực thi các lệnh sau để tạo CSDL và nạp dữ liệu mẫu:

```bash
# 1. Tạo database course_registration
sudo -u postgres psql -c "CREATE DATABASE course_registration;"

# 2. Chạy schema tạo cấu trúc bảng, ràng buộc và chỉ mục
sudo -u postgres psql -d course_registration -f database/schema.sql

# 3. Nạp dữ liệu mẫu và các tài khoản test
sudo -u postgres psql -d course_registration -f database/seed.sql

# 4. (Tùy chọn) Nạp thêm dữ liệu kiểm thử tài khoản bị khóa
sudo -u postgres psql -d course_registration -f database/test-data.sql
```

### Bước 2: Cấu hình thông số kết nối
* **Phía Server**: Mở file `server/src/main/resources/application.properties`, cập nhật mật khẩu PostgreSQL của máy bạn:
  ```properties
  db.url=jdbc:postgresql://localhost:5432/course_registration
  db.username=postgres
  db.password=YOUR_POSTGRES_PASSWORD
  server.port=9999
  server.max-clients=100
  ```
* **Phía Client**: Mở file `client/src/main/resources/application.properties` (chỉ chứa địa chỉ Server Socket, **không chứa cấu hình CSDL**):
  ```properties
  server.host=127.0.0.1
  server.port=9999
  ```

### Bước 3: Biên dịch toàn bộ dự án bằng Maven
Tại thư mục gốc của dự án:
```bash
mvn clean package
```
Lệnh này sẽ biên dịch toàn bộ 3 module và thực thi toàn bộ bộ kiểm thử tự động (Unit Test, Concurrency Race Condition Test). Kết quả trả về `BUILD SUCCESS`.

---

## 6. KHỞI CHẠY HỆ THỐNG

### Cách 1: Chạy Server với Giao diện Quản trị (Admin GUI Dashboard)
```bash
mvn exec:java -pl server
```
*Giao diện Admin Dashboard sẽ xuất hiện. Nhấn nút **[BẮT ĐẦU (START)]** để mở cổng ServerSocket 9999.*

*(Nếu chạy trên môi trường Linux Server không có màn hình GUI, thêm tham số `--headless` hoặc `--cli`):*
```bash
mvn exec:java -pl server -Dexec.args="--cli"
```

### Cách 2: Khởi chạy Client Sinh viên (Swing GUI)
Mở một cửa sổ Terminal mới và chạy:
```bash
mvn exec:java -pl client
```
*Có thể mở nhiều Terminal và chạy lệnh trên nhiều lần để giả lập nhiều Client sinh viên kết nối đồng thời!*

---

## 7. KỊCH BẢN DEMO ĐỒ ÁN (10–15 PHÚT)

1. **Khởi động Server**: Chạy Admin Dashboard, bấm Start Server. Nhìn console log ghi nhận `TCP Server started successfully on port 9999`.
2. **Khởi động Client A**: Chạy Client 1, đăng nhập với `sv01` / `123456`. Header hiển thị: "Sinh viên: Nguyễn Văn A", status: "Trực tuyến".
3. **Khởi động Client B**: Chạy Client 2, đăng nhập với `sv02` / `123456`. Header hiển thị: "Sinh viên: Trần Thị B".
4. **Kiểm tra Admin Dashboard**: Bảng "Online Clients" trên Admin Dashboard lập tức xuất hiện 2 dòng thông tin của `sv01` và `sv02` kèm IP, Port và thời gian online.
5. **Thử nghiệm Single-Session**: Mở Client 3, cố tình đăng nhập lại bằng `sv01`. Hệ thống từ chối ngay lập tức: `Tài khoản này hiện đang được đăng nhập ở một thiết bị khác`.
6. **Thử nghiệm Real-time Broadcast**:
   - Ở Client A: Tìm lớp `INT104_01` (45 slots), bấm `[ĐĂNG KÝ]`.
   - Quan sát Client B: Ô "Còn lại" của môn `INT104_01` trên bảng Client B lập tức nhảy về `44` mà không cần bấm F5 hay refresh.
7. **Thử nghiệm Phát hiện Trùng lịch (Schedule Conflict)**:
   - Ở Client A: Đã đăng ký môn `INT101_01` (Thứ 2, tiết 1-3).
   - Chọn đăng ký môn `INT102_01` (Thứ 2, tiết 2-4).
   - Server từ chối và báo lỗi: `Trùng lịch với môn 'Lập trình Mạng' (INT101_01) vào Thứ 2, tiết 1-3`.
8. **Thử nghiệm Phát hiện Trùng mã môn (Duplicate Subject)**:
   - Ở Client A: Đã có môn `INT101_01`.
   - Thử đăng ký môn `INT101_02` (cùng môn Lập trình mạng nhưng khác ca học).
   - Server từ chối: `Bạn đã đăng ký một lớp học phần khác thuộc môn học (INT101)`.
9. **Thử nghiệm Tranh chấp Slot cuối cùng (Race Condition)**:
   - Chọn lớp `COURSE_RACE_01` (chỉ còn đúng 1 slot duy nhất).
   - Cho Client A và Client B cùng bấm Đăng ký gần như đồng thời.
   - Kết quả: Đúng 1 Client thành công, Client còn lại nhận thông báo `Lớp học phần đã hết chỗ đăng ký`. Slot dừng lại ở `0` (không bao giờ âm).
10. **Thử nghiệm Admin Kick Client**:
    - Trên Admin Dashboard, chọn dòng của sinh viên `sv02`, bấm nút `[KICK CLIENT ĐÃ CHỌN]`.
    - Client B ngay lập tức nhận hộp thoại: `"Bạn đã bị quản trị viên đá khỏi hệ thống."` và bị đẩy văng về màn hình Đăng nhập.
11. **Thử nghiệm Admin Block Tài khoản**:
    - Nhập `sv01` vào ô Quản lý tài khoản, bấm `[KHÓA TÀI KHOẢN (BLOCK)]`.
    - Client A đang online bị Server ép `FORCE_LOGOUT` lập tức. Khi sinh viên cố tình đăng nhập lại, Server báo `Tài khoản đã bị vô hiệu hóa`.
12. **Xuất Thời khóa biểu Excel**:
    - Vào Tab "Học Phần Đã Đăng Ký", bấm `[Xuất Thời Khóa Biểu Ra Excel]`.
    - Mở file `.xlsx` kiểm tra 2 sheet (Thời khóa biểu dạng lưới và Danh sách chi tiết) được định dạng chuyên nghiệp.

---

## 8. TÀI LIỆU KÈM THEO

* [Kiến trúc hệ thống chi tiết](docs/architecture.md)
* [Đặc tả giao thức truyền thông TCP JSON](docs/protocol.md)
* [Từ điển dữ liệu CSDL PostgreSQL](docs/database.md)
* [Kịch bản và quy trình kiểm thử](docs/test-scenarios.md)
