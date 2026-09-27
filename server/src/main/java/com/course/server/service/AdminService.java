package com.course.server.service;

import com.course.common.dto.BlockUserRequestDto;
import com.course.common.dto.ServerStatusDto;
import com.course.common.model.User;
import com.course.common.protocol.Action;
import com.course.common.protocol.ErrorCode;
import com.course.common.protocol.Response;
import com.course.server.config.ServerConfig;
import com.course.server.network.ProtocolHandler;
import com.course.server.network.TcpServer;
import com.course.server.repository.UserRepository;
import com.course.server.session.SessionManager;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service providing administrative functions (account locking, force kick, server telemetry).
 */
public class AdminService implements ProtocolHandler.AdminDelegate {
    private static final Logger LOGGER = Logger.getLogger(AdminService.class.getName());

    private final UserRepository userRepository;
    private final SessionManager sessionManager;
    private TcpServer tcpServer;

    public AdminService() {
        this.userRepository = new UserRepository();
        this.sessionManager = SessionManager.getInstance();
    }

    public AdminService(UserRepository userRepository, SessionManager sessionManager) {
        this.userRepository = userRepository;
        this.sessionManager = sessionManager;
    }

    public void setTcpServer(TcpServer tcpServer) {
        this.tcpServer = tcpServer;
    }

    @Override
    public Response blockUser(BlockUserRequestDto req, String requestId) {
        String identifier = req.getIdentifier();
        boolean block = req.isBlocked();

        try {
            Optional<User> userOpt = userRepository.findByIdentifier(identifier);
            if (userOpt.isEmpty()) {
                return Response.error(Action.BLOCK_USER_RESPONSE, requestId, ErrorCode.INVALID_REQUEST,
                        "Không tìm thấy người dùng với định danh: " + identifier);
            }

            User user = userOpt.get();
            boolean newActiveState = !block;
            boolean updated = userRepository.updateIsActive(user.getStudentId(), newActiveState);

            if (!updated) {
                return Response.error(Action.BLOCK_USER_RESPONSE, requestId, ErrorCode.DATABASE_ERROR,
                        "Không thể cập nhật trạng thái tài khoản người dùng.");
            }

            // If blocking user and user is currently online, enforce immediate FORCE_LOGOUT!
            if (block && sessionManager.isStudentOnline(user.getStudentId())) {
                LOGGER.info("User " + user.getStudentId() + " is online. Forcing logout due to account block.");
                sessionManager.kickSession(user.getStudentId(), "Tài khoản của bạn đã bị khóa bởi quản trị viên hệ thống.");
            }

            String actionText = block ? "Khóa tài khoản" : "Mở khóa tài khoản";
            LOGGER.info(String.format("Admin executed %s for student %s (%s)", actionText, user.getStudentId(), user.getUsername()));
            return Response.success(Action.BLOCK_USER_RESPONSE, requestId, actionText + " thành công cho " + user.getFullName());

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to block/unblock user: " + identifier, e);
            return Response.error(Action.BLOCK_USER_RESPONSE, requestId, ErrorCode.SERVER_ERROR, "Lỗi máy chủ khi thay đổi trạng thái tài khoản.");
        }
    }

    @Override
    public Response getServerStatus(String requestId) {
        try {
            boolean running = tcpServer != null && tcpServer.isRunning();
            int port = tcpServer != null ? tcpServer.getPort() : ServerConfig.getInstance().getServerPort();
            int online = sessionManager.getOnlineCount();
            int max = ServerConfig.getInstance().getServerMaxClients();
            LocalDateTime started = tcpServer != null ? tcpServer.getStartedAt() : LocalDateTime.now();

            ServerStatusDto dto = new ServerStatusDto(running, port, online, max, started);
            return Response.success(Action.SERVER_STATUS_RESPONSE, requestId, "Trạng thái máy chủ", dto);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to get server status", e);
            return Response.error(Action.SERVER_STATUS_RESPONSE, requestId, ErrorCode.SERVER_ERROR, "Không thể lấy trạng thái máy chủ.");
        }
    }
}
