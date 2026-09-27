package com.course.server.network;

import com.course.common.dto.*;
import com.course.common.model.Role;
import com.course.common.protocol.Action;
import com.course.common.protocol.ErrorCode;
import com.course.common.protocol.Request;
import com.course.common.protocol.Response;
import com.course.server.session.ClientSession;
import com.course.server.session.SessionManager;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dispatches and authorizes incoming protocol requests from clients.
 */
public class ProtocolHandler {
    private static final Logger LOGGER = Logger.getLogger(ProtocolHandler.class.getName());
    private static final ProtocolHandler INSTANCE = new ProtocolHandler();

    // Delegate service interfaces to be connected in Part 5
    public interface AuthDelegate {
        Response login(LoginRequestDto req, ClientSession session, String requestId);
    }

    public interface CourseDelegate {
        Response getCourses(String requestId);
        Response searchCourses(SearchCourseRequestDto req, String requestId);
    }

    public interface EnrollmentDelegate {
        Response getMyEnrollments(String studentId, String requestId);
        Response registerCourse(String studentId, RegisterCourseRequestDto req, String requestId);
        Response cancelCourse(String studentId, CancelCourseRequestDto req, String requestId);
    }

    public interface AdminDelegate {
        Response blockUser(BlockUserRequestDto req, String requestId);
        Response getServerStatus(String requestId);
    }

    private AuthDelegate authDelegate;
    private CourseDelegate courseDelegate;
    private EnrollmentDelegate enrollmentDelegate;
    private AdminDelegate adminDelegate;

    private ProtocolHandler() {
    }

    public static ProtocolHandler getInstance() {
        return INSTANCE;
    }

    public void setAuthDelegate(AuthDelegate authDelegate) {
        this.authDelegate = authDelegate;
    }

    public void setCourseDelegate(CourseDelegate courseDelegate) {
        this.courseDelegate = courseDelegate;
    }

    public void setEnrollmentDelegate(EnrollmentDelegate enrollmentDelegate) {
        this.enrollmentDelegate = enrollmentDelegate;
    }

    public void setAdminDelegate(AdminDelegate adminDelegate) {
        this.adminDelegate = adminDelegate;
    }

    /**
     * Processes an incoming request, validates authentication/authorization,
     * and delegates to the appropriate service handler.
     */
    public Response handle(ClientSession session, Request request) {
        if (request == null || request.getAction() == null) {
            return Response.error(Action.ERROR, null, ErrorCode.INVALID_REQUEST, "Yêu cầu không hợp lệ hoặc thiếu action.");
        }

        Action action = request.getAction();
        String reqId = request.getRequestId();

        try {
            // 1. Heartbeat action (no auth needed)
            if (action == Action.PING) {
                session.updateHeartbeat();
                return Response.success(Action.PONG, reqId, "PONG");
            }

            // 2. Authentication action
            if (action == Action.LOGIN) {
                LoginRequestDto dto = request.getDataAs(LoginRequestDto.class);
                if (dto == null || dto.getUsername() == null || dto.getPassword() == null) {
                    return Response.error(Action.LOGIN_RESPONSE, reqId, ErrorCode.INVALID_REQUEST, "Thiếu tên đăng nhập hoặc mật khẩu.");
                }
                if (authDelegate != null) {
                    return authDelegate.login(dto, session, reqId);
                }
                return Response.error(Action.LOGIN_RESPONSE, reqId, ErrorCode.SERVER_ERROR, "Dịch vụ xác thực chưa sẵn sàng.");
            }

            // 3. For all other actions, verify authentication
            if (session.getRole() == null) {
                return Response.error(action, reqId, ErrorCode.NOT_AUTHENTICATED, "Vui lòng đăng nhập trước khi thực hiện thao tác.");
            }

            // 4. Logout action
            if (action == Action.LOGOUT) {
                SessionManager.getInstance().unbindUser(session.getSessionId());
                return Response.success(Action.LOGOUT_RESPONSE, reqId, "Đăng xuất thành công.");
            }

            // 5. Admin-only actions
            if (isAdminAction(action)) {
                if (session.getRole() != Role.ADMIN) {
                    LOGGER.warning("Access denied: User " + session.getStudentId() + " attempted admin action " + action);
                    return Response.error(action, reqId, ErrorCode.ACCESS_DENIED, "Chỉ quản trị viên mới có quyền thực hiện chức năng này.");
                }
                return handleAdminAction(session, action, reqId, request);
            }

            // 6. Student actions
            if (session.getRole() != Role.STUDENT && session.getRole() != Role.ADMIN) {
                return Response.error(action, reqId, ErrorCode.ACCESS_DENIED, "Quyền truy cập không hợp lệ.");
            }

            return handleStudentAction(session, action, reqId, request);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error processing request " + action + " (ID: " + reqId + ")", e);
            return Response.error(action, reqId, ErrorCode.SERVER_ERROR, "Lỗi xử lý hệ thống: " + e.getMessage());
        }
    }

    private boolean isAdminAction(Action action) {
        return action == Action.GET_ONLINE_CLIENTS ||
               action == Action.KICK_CLIENT ||
               action == Action.BLOCK_USER ||
               action == Action.UNBLOCK_USER ||
               action == Action.SERVER_STATUS;
    }

    private Response handleAdminAction(ClientSession session, Action action, String reqId, Request request) {
        switch (action) {
            case GET_ONLINE_CLIENTS:
                return Response.success(Action.GET_ONLINE_CLIENTS_RESPONSE, reqId,
                        "Danh sách client online", SessionManager.getInstance().getAllSessionDtos());

            case KICK_CLIENT:
                KickClientRequestDto kickDto = request.getDataAs(KickClientRequestDto.class);
                if (kickDto == null || (kickDto.getStudentId() == null && kickDto.getSessionId() == null)) {
                    return Response.error(Action.KICK_CLIENT_RESPONSE, reqId, ErrorCode.INVALID_REQUEST, "Thiếu định danh client cần kick.");
                }
                String targetId = kickDto.getStudentId() != null ? kickDto.getStudentId() : kickDto.getSessionId();
                boolean kicked = SessionManager.getInstance().kickSession(targetId, kickDto.getReason());
                if (kicked) {
                    return Response.success(Action.KICK_CLIENT_RESPONSE, reqId, "Đã ngắt kết nối client thành công.");
                } else {
                    return Response.error(Action.KICK_CLIENT_RESPONSE, reqId, ErrorCode.INVALID_REQUEST, "Client không còn trực tuyến hoặc không tồn tại.");
                }

            case BLOCK_USER:
            case UNBLOCK_USER:
                BlockUserRequestDto blockDto = request.getDataAs(BlockUserRequestDto.class);
                if (blockDto == null || blockDto.getIdentifier() == null) {
                    return Response.error(action, reqId, ErrorCode.INVALID_REQUEST, "Thiếu thông tin người dùng.");
                }
                if (adminDelegate != null) {
                    return adminDelegate.blockUser(blockDto, reqId);
                }
                return Response.error(action, reqId, ErrorCode.SERVER_ERROR, "Dịch vụ quản trị chưa sẵn sàng.");

            case SERVER_STATUS:
                if (adminDelegate != null) {
                    return adminDelegate.getServerStatus(reqId);
                }
                return Response.error(Action.SERVER_STATUS_RESPONSE, reqId, ErrorCode.SERVER_ERROR, "Dịch vụ chưa sẵn sàng.");

            default:
                return Response.error(action, reqId, ErrorCode.INVALID_REQUEST, "Hành động quản trị không được hỗ trợ.");
        }
    }

    private Response handleStudentAction(ClientSession session, Action action, String reqId, Request request) {
        String studentId = session.getStudentId();

        switch (action) {
            case GET_COURSES:
                if (courseDelegate != null) {
                    return courseDelegate.getCourses(reqId);
                }
                return Response.error(Action.GET_COURSES_RESPONSE, reqId, ErrorCode.SERVER_ERROR, "Dịch vụ học phần chưa sẵn sàng.");

            case SEARCH_COURSES:
                SearchCourseRequestDto searchDto = request.getDataAs(SearchCourseRequestDto.class);
                if (searchDto == null) {
                    searchDto = new SearchCourseRequestDto("", SearchCourseRequestDto.SearchType.ALL);
                }
                if (courseDelegate != null) {
                    return courseDelegate.searchCourses(searchDto, reqId);
                }
                return Response.error(Action.SEARCH_COURSES_RESPONSE, reqId, ErrorCode.SERVER_ERROR, "Dịch vụ tìm kiếm chưa sẵn sàng.");

            case GET_MY_ENROLLMENTS:
                if (enrollmentDelegate != null) {
                    return enrollmentDelegate.getMyEnrollments(studentId, reqId);
                }
                return Response.error(Action.GET_MY_ENROLLMENTS_RESPONSE, reqId, ErrorCode.SERVER_ERROR, "Dịch vụ đăng ký chưa sẵn sàng.");

            case REGISTER_COURSE:
                RegisterCourseRequestDto regDto = request.getDataAs(RegisterCourseRequestDto.class);
                if (regDto == null || regDto.getCourseId() == null || regDto.getCourseId().isBlank()) {
                    return Response.error(Action.REGISTER_COURSE_RESPONSE, reqId, ErrorCode.INVALID_REQUEST, "Mã lớp học phần không được để trống.");
                }
                if (enrollmentDelegate != null) {
                    return enrollmentDelegate.registerCourse(studentId, regDto, reqId);
                }
                return Response.error(Action.REGISTER_COURSE_RESPONSE, reqId, ErrorCode.SERVER_ERROR, "Dịch vụ đăng ký chưa sẵn sàng.");

            case CANCEL_COURSE:
                CancelCourseRequestDto cancelDto = request.getDataAs(CancelCourseRequestDto.class);
                if (cancelDto == null || cancelDto.getCourseId() == null || cancelDto.getCourseId().isBlank()) {
                    return Response.error(Action.CANCEL_COURSE_RESPONSE, reqId, ErrorCode.INVALID_REQUEST, "Mã lớp học phần không được để trống.");
                }
                if (enrollmentDelegate != null) {
                    return enrollmentDelegate.cancelCourse(studentId, cancelDto, reqId);
                }
                return Response.error(Action.CANCEL_COURSE_RESPONSE, reqId, ErrorCode.SERVER_ERROR, "Dịch vụ đăng ký chưa sẵn sàng.");

            default:
                return Response.error(action, reqId, ErrorCode.INVALID_REQUEST, "Hành động không được hỗ trợ.");
        }
    }
}
