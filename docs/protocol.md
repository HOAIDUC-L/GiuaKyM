# GIAO THỨC TRUYỀN THÔNG TCP SOCKET (JSON PROTOCOL SPECIFICATION)

## 1. ĐẶC TẢ TẦNG MẠNG & THÔNG ĐIỆP (TCP MESSAGE FRAMING)

* **Giao thức truyền vận**: TCP Socket (`java.net.Socket`, `java.net.ServerSocket`).
* **Định dạng dữ liệu**: JSON UTF-8.
* **Phân khung thông điệp (Framing)**: Ký tự xuống dòng `\n`.
  - Phía Client/Server đọc thông điệp bằng: `BufferedReader.readLine()`.
  - Phía Client/Server ghi thông điệp bằng: `PrintWriter.println(jsonString)`.
* **Cơ chế bất đối xứng**: Mỗi thông điệp trao đổi là một đối tượng JSON độc lập, không phụ thuộc vào kích thước buffer socket.

---

## 2. CẤU TRÚC GÓI TIN CHUẨN

### 2.1. Cấu trúc Yêu cầu (Request)
```json
{
  "action": "ACTION_NAME",
  "requestId": "REQ-xxxx",
  "data": { ... }
}
```

### 2.2. Cấu trúc Phản hồi (Response)
```json
{
  "action": "ACTION_NAME_RESPONSE",
  "requestId": "REQ-xxxx",
  "success": true,
  "errorCode": null,
  "message": "Thông điệp mô tả kết quả",
  "data": { ... }
}
```

### 2.3. Cấu trúc Đẩy dữ liệu từ Server (Server Push / Broadcast)
*(Không bắt buộc trường `requestId`)*
```json
{
  "action": "COURSE_UPDATED",
  "data": {
    "courseId": "INT101_01",
    "remainingSlots": 39
  }
}
```

---

## 3. DANH SÁCH MÃ LỖI (ERROR CODES)

| Mã lỗi (`errorCode`) | Ý nghĩa |
| :--- | :--- |
| `INVALID_REQUEST` | Cú pháp JSON hoặc tham số dữ liệu không hợp lệ. |
| `AUTH_FAILED` | Tên đăng nhập hoặc mật khẩu không chính xác. |
| `ACCOUNT_DISABLED` | Tài khoản đã bị quản trị viên khóa/vô hiệu hóa. |
| `ALREADY_LOGGED_IN` | Vi phạm Single-Session: Tài khoản đang online trên máy khác. |
| `ACCESS_DENIED` | Người dùng không đủ quyền thực hiện hành động này. |
| `NOT_AUTHENTICATED` | Chưa đăng nhập vào hệ thống. |
| `COURSE_NOT_FOUND` | Lớp học phần được yêu cầu không tồn tại. |
| `COURSE_FULL` | Lớp học phần đã hết chỗ đăng ký (`remaining_slots = 0`). |
| `ALREADY_ENROLLED` | Sinh viên đã đăng ký chính lớp học phần này trước đó. |
| `DUPLICATE_SUBJECT` | Đã đăng ký một lớp học phần khác thuộc cùng mã môn học (`subject_code`). |
| `SCHEDULE_CONFLICT` | Trùng lịch học (cùng thứ và trùng khoảng tiết học). |
| `ENROLLMENT_NOT_FOUND` | Không tìm thấy bản ghi đăng ký tương ứng để hủy. |
| `SERVER_ERROR` | Lỗi nội bộ không mong muốn phía máy chủ. |
| `DATABASE_ERROR` | Lỗi thao tác cơ sở dữ liệu PostgreSQL. |

---

## 4. CHI TIẾT CÁC HÀNH ĐỘNG (ACTIONS)

### 4.1. Đăng nhập (`LOGIN`)
* **Request**:
```json
{
  "action": "LOGIN",
  "requestId": "REQ-001",
  "data": {
    "username": "sv01",
    "password": "123456"
  }
}
```
* **Response (Thành công)**:
```json
{
  "action": "LOGIN_RESPONSE",
  "requestId": "REQ-001",
  "success": true,
  "message": "Đăng nhập thành công.",
  "data": {
    "studentId": "SV001",
    "username": "sv01",
    "fullName": "Nguyễn Văn A",
    "email": "sv01@student.ptit.edu.vn",
    "className": "D21CQCN01-B",
    "role": "STUDENT"
  }
}
```

### 4.2. Đăng xuất (`LOGOUT`)
* **Request**:
```json
{
  "action": "LOGOUT",
  "requestId": "REQ-002",
  "data": null
}
```
* **Response**:
```json
{
  "action": "LOGOUT_RESPONSE",
  "requestId": "REQ-002",
  "success": true,
  "message": "Đăng xuất thành công."
}
```

### 4.3. Giữ kết nối Heartbeat (`PING` / `PONG`)
* **Request**: `{"action":"PING","requestId":"REQ-PING","data":"PING"}`
* **Response**: `{"action":"PONG","requestId":"REQ-PING","success":true,"message":"PONG"}`

### 4.4. Lấy danh sách lớp học phần (`GET_COURSES`)
* **Request**: `{"action":"GET_COURSES","requestId":"REQ-003","data":null}`
* **Response**:
```json
{
  "action": "GET_COURSES_RESPONSE",
  "requestId": "REQ-003",
  "success": true,
  "message": "Lấy danh sách học phần thành công.",
  "data": [
    {
      "courseId": "INT101_01",
      "subjectCode": "INT101",
      "courseName": "Lập trình Mạng",
      "lecturerName": "TS. Nguyễn Văn D",
      "dayOfWeek": 2,
      "startPeriod": 1,
      "endPeriod": 3,
      "room": "301-A2",
      "maxSlots": 40,
      "remainingSlots": 40
    }
  ]
}
```

### 4.5. Tìm kiếm lớp học phần (`SEARCH_COURSES`)
* **Request**:
```json
{
  "action": "SEARCH_COURSES",
  "requestId": "REQ-004",
  "data": {
    "keyword": "Mạng",
    "searchType": "ALL"
  }
}
```

### 4.6. Lấy danh sách học phần đã đăng ký (`GET_MY_ENROLLMENTS`)
* **Request**: `{"action":"GET_MY_ENROLLMENTS","requestId":"REQ-005","data":null}`

### 4.7. Đăng ký học phần (`REGISTER_COURSE`)
* **Request**:
```json
{
  "action": "REGISTER_COURSE",
  "requestId": "REQ-006",
  "data": {
    "courseId": "INT101_01"
  }
}
```
* **Response Thành công**:
```json
{
  "action": "REGISTER_COURSE_RESPONSE",
  "requestId": "REQ-006",
  "success": true,
  "message": "Đăng ký học phần thành công.",
  "data": {
    "courseId": "INT101_01",
    "remainingSlots": 39
  }
}
```
* **Response Trùng lịch (Thất bại)**:
```json
{
  "action": "REGISTER_COURSE_RESPONSE",
  "requestId": "REQ-006",
  "success": false,
  "errorCode": "SCHEDULE_CONFLICT",
  "message": "Trùng lịch với môn 'Lập trình Mạng' (INT101_01) vào Thứ 2, tiết 1-3."
}
```

### 4.8. Hủy đăng ký học phần (`CANCEL_COURSE`)
* **Request**:
```json
{
  "action": "CANCEL_COURSE",
  "requestId": "REQ-007",
  "data": {
    "courseId": "INT101_01"
  }
}
```

### 4.9. Phát sóng cập nhật Slot tức thời (`COURSE_UPDATED` - Server Push)
```json
{
  "action": "COURSE_UPDATED",
  "success": true,
  "data": {
    "courseId": "INT101_01",
    "remainingSlots": 39
  }
}
```

### 4.10. Ép đăng xuất (`FORCE_LOGOUT` - Server Push)
```json
{
  "action": "FORCE_LOGOUT",
  "success": false,
  "errorCode": "ACCOUNT_DISABLED",
  "message": "Bạn đã bị quản trị viên ngắt kết nối khỏi hệ thống."
}
```

### 4.11. Quản trị: Xem Client Online (`GET_ONLINE_CLIENTS`)
* **Chỉ ADMIN được phép gọi**.

### 4.12. Quản trị: Kick Client (`KICK_CLIENT`)
* **Request**:
```json
{
  "action": "KICK_CLIENT",
  "requestId": "REQ-008",
  "data": {
    "studentId": "SV001",
    "reason": "Vi phạm quy chế thi"
  }
}
```

### 4.13. Quản trị: Khóa / Mở khóa tài khoản (`BLOCK_USER` / `UNBLOCK_USER`)
* **Request**:
```json
{
  "action": "BLOCK_USER",
  "requestId": "REQ-009",
  "data": {
    "identifier": "SV001",
    "blocked": true
  }
}
```
