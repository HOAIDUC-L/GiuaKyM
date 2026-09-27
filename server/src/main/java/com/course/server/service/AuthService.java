package com.course.server.service;

import com.course.common.dto.LoginRequestDto;
import com.course.common.dto.LoginResponseDto;
import com.course.common.model.User;
import com.course.common.protocol.Action;
import com.course.common.protocol.ErrorCode;
import com.course.common.protocol.Response;
import com.course.server.network.ProtocolHandler;
import com.course.server.repository.SessionLogRepository;
import com.course.server.repository.UserRepository;
import com.course.server.session.ClientSession;
import com.course.server.session.SessionManager;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service managing user authentication, BCrypt verification, and session binding.
 */
public class AuthService implements ProtocolHandler.AuthDelegate {
    private static final Logger LOGGER = Logger.getLogger(AuthService.class.getName());

    private final UserRepository userRepository;
    private final SessionLogRepository sessionLogRepository;
    private final SessionManager sessionManager;

    public AuthService() {
        this.userRepository = new UserRepository();
        this.sessionLogRepository = new SessionLogRepository();
        this.sessionManager = SessionManager.getInstance();

        // Hook disconnect listener to log logout/disconnect in DB
        this.sessionManager.setOnDisconnectListener(session -> {
            if (session.getStudentId() != null) {
                sessionLogRepository.createLog(session.getStudentId(), session.getClientIp(), session.getClientPort());
            }
        });
    }

    public AuthService(UserRepository userRepository, SessionLogRepository sessionLogRepository, SessionManager sessionManager) {
        this.userRepository = userRepository;
        this.sessionLogRepository = sessionLogRepository;
        this.sessionManager = sessionManager;
    }

    @Override
    public Response login(LoginRequestDto req, ClientSession session, String requestId) {
        if (req == null || req.getUsername() == null || req.getPassword() == null) {
            return Response.error(Action.LOGIN_RESPONSE, requestId, ErrorCode.INVALID_REQUEST, "Tên đăng nhập và mật khẩu không được để trống.");
        }

        String username = req.getUsername().trim();
        String plainPassword = req.getPassword();

        try {
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isEmpty()) {
                LOGGER.info("Login failed: Unknown username " + username);
                return Response.error(Action.LOGIN_RESPONSE, requestId, ErrorCode.AUTH_FAILED, "Tài khoản hoặc mật khẩu không chính xác.");
            }

            User user = userOpt.get();

            // Verify password using BCrypt
            if (!BCrypt.checkpw(plainPassword, user.getPassword())) {
                LOGGER.info("Login failed: Invalid password for user " + username);
                return Response.error(Action.LOGIN_RESPONSE, requestId, ErrorCode.AUTH_FAILED, "Tài khoản hoặc mật khẩu không chính xác.");
            }

            // Check if account is disabled
            if (!user.isActive()) {
                LOGGER.warning("Login failed: Account disabled for user " + username);
                return Response.error(Action.LOGIN_RESPONSE, requestId, ErrorCode.ACCOUNT_DISABLED, "Tài khoản của bạn đã bị vô hiệu hóa hoặc bị khóa bởi quản trị viên.");
            }

            // Enforce single session policy
            boolean bound = sessionManager.bindUser(
                    user.getStudentId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getFullName(),
                    user.getRole(),
                    session.getSessionId()
            );

            if (!bound) {
                LOGGER.warning("Login rejected: Account " + user.getStudentId() + " already active in another session.");
                return Response.error(Action.LOGIN_RESPONSE, requestId, ErrorCode.ALREADY_LOGGED_IN, "Tài khoản này hiện đang được đăng nhập ở một thiết bị khác.");
            }

            // Log session entry to database
            Long logId = sessionLogRepository.createLog(user.getStudentId(), session.getClientIp(), session.getClientPort());

            LOGGER.info(String.format("Login successful: %s (%s, Role: %s)", user.getUsername(), user.getStudentId(), user.getRole()));

            LoginResponseDto responseDto = new LoginResponseDto(
                    user.getStudentId(),
                    user.getUsername(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getClassName(),
                    user.getRole()
            );

            return Response.success(Action.LOGIN_RESPONSE, requestId, "Đăng nhập thành công.", responseDto);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error during login for user " + username, e);
            return Response.error(Action.LOGIN_RESPONSE, requestId, ErrorCode.SERVER_ERROR, "Lỗi máy chủ trong quá trình đăng nhập.");
        }
    }
}
