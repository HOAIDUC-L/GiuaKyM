package com.course.server;

import com.course.server.database.DatabaseManager;
import com.course.server.network.ProtocolHandler;
import com.course.server.network.TcpServer;
import com.course.server.service.AdminService;
import com.course.server.service.AuthService;
import com.course.server.service.CourseService;
import com.course.server.service.EnrollmentService;

import java.util.logging.Logger;

/**
 * Bootstrapper and dependency wiring container for Server services.
 */
public class ServerContext {
    private static final Logger LOGGER = Logger.getLogger(ServerContext.class.getName());
    private static final ServerContext INSTANCE = new ServerContext();

    private AuthService authService;
    private CourseService courseService;
    private EnrollmentService enrollmentService;
    private AdminService adminService;
    private TcpServer tcpServer;
    private boolean initialized = false;

    private ServerContext() {
    }

    public static ServerContext getInstance() {
        return INSTANCE;
    }

    public synchronized void initialize(TcpServer server) {
        if (initialized) {
            this.tcpServer = server;
            if (adminService != null) {
                adminService.setTcpServer(server);
            }
            return;
        }

        LOGGER.info("Initializing Server context and services...");

        // Eagerly initialize Database connection pool
        DatabaseManager.getInstance();

        this.authService = new AuthService();
        this.courseService = new CourseService();
        this.enrollmentService = new EnrollmentService();
        this.adminService = new AdminService();
        this.tcpServer = server;

        if (server != null) {
            this.adminService.setTcpServer(server);
        }

        // Wire delegates into ProtocolHandler
        ProtocolHandler handler = ProtocolHandler.getInstance();
        handler.setAuthDelegate(authService);
        handler.setCourseDelegate(courseService);
        handler.setEnrollmentDelegate(enrollmentService);
        handler.setAdminDelegate(adminService);

        this.initialized = true;
        LOGGER.info("Server context initialized successfully.");
    }

    public AuthService getAuthService() {
        return authService;
    }

    public CourseService getCourseService() {
        return courseService;
    }

    public EnrollmentService getEnrollmentService() {
        return enrollmentService;
    }

    public AdminService getAdminService() {
        return adminService;
    }

    public TcpServer getTcpServer() {
        return tcpServer;
    }
}
