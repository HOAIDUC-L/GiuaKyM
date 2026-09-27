package com.course.client.service;

import com.course.client.config.ClientConfig;
import com.course.client.network.ClientSocketManager;
import com.course.client.network.ServerPushListener;
import com.course.client.state.ClientState;
import com.course.common.dto.*;
import com.course.common.model.Course;
import com.course.common.protocol.Action;
import com.course.common.protocol.ErrorCode;
import com.course.common.protocol.Request;
import com.course.common.protocol.Response;
import com.fasterxml.jackson.core.type.TypeReference;
import com.course.common.util.JsonUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * High-level business facade for the Client Swing GUI.
 * Translates UI intents into TCP Socket requests.
 */
public class ClientService {
    private static final Logger LOGGER = Logger.getLogger(ClientService.class.getName());
    private static final ClientService INSTANCE = new ClientService();

    private final ClientSocketManager socketManager;
    private final ClientState clientState;
    private final long timeoutMs;

    private ClientService() {
        this.socketManager = ClientSocketManager.getInstance();
        this.clientState = ClientState.getInstance();
        this.timeoutMs = ClientConfig.getInstance().getRequestTimeoutMs();

        // Listen for realtime push updates
        this.socketManager.addPushListener(new ServerPushListener() {
            @Override
            public void onCourseUpdated(String courseId, int remainingSlots) {
                clientState.updateCourseSlot(courseId, remainingSlots);
            }

            @Override
            public void onForceLogout(String message) {
                clientState.clear();
            }

            @Override
            public void onDisconnected(String reason) {
                LOGGER.info("Disconnected from server: " + reason);
            }
        });
    }

    public static ClientService getInstance() {
        return INSTANCE;
    }

    private void ensureConnected() throws IOException {
        if (!socketManager.isConnected()) {
            ClientConfig config = ClientConfig.getInstance();
            socketManager.connect(config.getServerHost(), config.getServerPort());
        }
    }

    public Response login(String username, String password) {
        try {
            ensureConnected();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Cannot connect to server", e);
            return Response.error(Action.LOGIN_RESPONSE, null, ErrorCode.SERVER_ERROR,
                    "Không thể kết nối đến máy chủ: " + e.getMessage());
        }

        LoginRequestDto dto = new LoginRequestDto(username, password);
        Request request = Request.of(Action.LOGIN, dto);
        Response response = socketManager.sendRequest(request, timeoutMs);

        if (response.isSuccess()) {
            LoginResponseDto user = response.getDataAs(LoginResponseDto.class);
            clientState.setCurrentUser(user);
        }
        return response;
    }

    public Response logout() {
        if (!socketManager.isConnected() || !clientState.isLoggedIn()) {
            clientState.clear();
            return Response.success(Action.LOGOUT_RESPONSE, null, "Đã đăng xuất.");
        }

        Request request = Request.of(Action.LOGOUT, null);
        Response response = socketManager.sendRequest(request, 3000);
        clientState.clear();
        socketManager.disconnect();
        return response;
    }

    public List<Course> getCourses() {
        try {
            ensureConnected();
            Request request = Request.of(Action.GET_COURSES, null);
            Response response = socketManager.sendRequest(request, timeoutMs);

            if (response.isSuccess() && response.getData() != null) {
                List<Course> courses = JsonUtil.fromJson(response.getData().toString(), new TypeReference<List<Course>>() {});
                clientState.setCachedCourses(courses);
                return courses;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load courses from server", e);
        }
        return Collections.emptyList();
    }

    public List<Course> searchCourses(String keyword, SearchCourseRequestDto.SearchType searchType) {
        try {
            ensureConnected();
            SearchCourseRequestDto dto = new SearchCourseRequestDto(keyword, searchType);
            Request request = Request.of(Action.SEARCH_COURSES, dto);
            Response response = socketManager.sendRequest(request, timeoutMs);

            if (response.isSuccess() && response.getData() != null) {
                List<Course> courses = JsonUtil.fromJson(response.getData().toString(), new TypeReference<List<Course>>() {});
                clientState.setCachedCourses(courses);
                return courses;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to search courses", e);
        }
        return Collections.emptyList();
    }

    public List<EnrollmentDetailDto> getMyEnrollments() {
        try {
            ensureConnected();
            Request request = Request.of(Action.GET_MY_ENROLLMENTS, null);
            Response response = socketManager.sendRequest(request, timeoutMs);

            if (response.isSuccess() && response.getData() != null) {
                List<EnrollmentDetailDto> list = JsonUtil.fromJson(response.getData().toString(), new TypeReference<List<EnrollmentDetailDto>>() {});
                clientState.setCachedEnrollments(list);
                return list;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load my enrollments", e);
        }
        return Collections.emptyList();
    }

    public Response registerCourse(String courseId) {
        try {
            ensureConnected();
            RegisterCourseRequestDto dto = new RegisterCourseRequestDto(courseId);
            Request request = Request.of(Action.REGISTER_COURSE, dto);
            Response response = socketManager.sendRequest(request, timeoutMs);

            if (response.isSuccess()) {
                // Refresh my enrollments after successful registration
                getMyEnrollments();
            }
            return response;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error registering course " + courseId, e);
            return Response.error(Action.REGISTER_COURSE_RESPONSE, null, ErrorCode.SERVER_ERROR,
                    "Lỗi gửi yêu cầu đăng ký: " + e.getMessage());
        }
    }

    public Response cancelCourse(String courseId) {
        try {
            ensureConnected();
            CancelCourseRequestDto dto = new CancelCourseRequestDto(courseId);
            Request request = Request.of(Action.CANCEL_COURSE, dto);
            Response response = socketManager.sendRequest(request, timeoutMs);

            if (response.isSuccess()) {
                // Refresh my enrollments after cancellation
                getMyEnrollments();
            }
            return response;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error cancelling course " + courseId, e);
            return Response.error(Action.CANCEL_COURSE_RESPONSE, null, ErrorCode.SERVER_ERROR,
                    "Lỗi gửi yêu cầu hủy đăng ký: " + e.getMessage());
        }
    }
}
