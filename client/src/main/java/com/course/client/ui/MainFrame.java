package com.course.client.ui;

import com.course.client.network.ClientSocketManager;
import com.course.client.network.ServerPushListener;
import com.course.client.service.ClientService;
import com.course.client.state.ClientState;
import com.course.client.ui.util.UiUtil;
import com.course.common.dto.EnrollmentDetailDto;
import com.course.common.dto.LoginResponseDto;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

/**
 * Main application window for authenticated students.
 */
public class MainFrame extends JFrame implements ServerPushListener {
    private final LoginResponseDto currentUser;

    private final CoursePanel coursePanel;
    private final MyEnrollmentsPanel myEnrollmentsPanel;
    private final ScheduleMatrixPanel scheduleMatrixPanel;
    private final JTabbedPane tabbedPane;

    public MainFrame(LoginResponseDto user) {
        this.currentUser = user;
        UiUtil.setupLookAndFeel();

        setTitle("Hệ Thống Đăng Ký Học Phần - Sinh Viên: " + (user != null ? user.getFullName() : ""));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1100, 720);
        setMinimumSize(new Dimension(950, 600));
        setLocationRelativeTo(null);

        // Window closing confirmation and graceful logout
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleLogout();
            }
        });

        JPanel contentPane = new JPanel(new BorderLayout());
        setContentPane(contentPane);

        // 1. Header Bar
        JPanel headerBar = createHeaderBar();
        contentPane.add(headerBar, BorderLayout.NORTH);

        // 2. Coordinated components
        Runnable refreshCallback = this::syncEnrollmentsAndSchedule;

        this.coursePanel = new CoursePanel(refreshCallback);
        this.myEnrollmentsPanel = new MyEnrollmentsPanel(refreshCallback);
        this.scheduleMatrixPanel = new ScheduleMatrixPanel();

        // 3. Tabbed Pane
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UiUtil.TITLE_FONT);
        tabbedPane.addTab("  Đăng Ký Học Phần  ", coursePanel);
        tabbedPane.addTab("  Học Phần Đã Đăng Ký  ", myEnrollmentsPanel);
        tabbedPane.addTab("  Thời Khóa Biểu Tuần  ", scheduleMatrixPanel);

        // When switching tabs, ensure schedule matrix is up to date
        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getSelectedComponent() == scheduleMatrixPanel) {
                scheduleMatrixPanel.updateSchedule(myEnrollmentsPanel.getCurrentEnrollments());
            }
        });

        contentPane.add(tabbedPane, BorderLayout.CENTER);

        // Register push listener for FORCE_LOGOUT
        ClientSocketManager.getInstance().addPushListener(this);

        // Initial sync of schedule matrix
        syncEnrollmentsAndSchedule();
    }

    private JPanel createHeaderBar() {
        JPanel header = new JPanel(new BorderLayout(15, 10));
        header.setBorder(new EmptyBorder(10, 15, 10, 15));
        header.setBackground(new Color(245, 247, 250));

        // Left info
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        infoPanel.setOpaque(false);

        JLabel lblName = new JLabel("Sinh viên: " + (currentUser != null ? currentUser.getFullName() : "N/A"));
        lblName.setFont(UiUtil.TITLE_FONT);

        JLabel lblMssv = new JLabel("MSSV: " + (currentUser != null ? currentUser.getStudentId() : "N/A"));
        lblMssv.setFont(UiUtil.REGULAR_FONT);

        JLabel lblClass = new JLabel("Lớp: " + (currentUser != null && currentUser.getClassName() != null ? currentUser.getClassName() : "N/A"));
        lblClass.setFont(UiUtil.REGULAR_FONT);

        JLabel lblStatus = new JLabel("● Trực tuyến");
        lblStatus.setFont(UiUtil.TITLE_FONT);
        lblStatus.setForeground(UiUtil.SUCCESS_COLOR);

        infoPanel.add(lblName);
        infoPanel.add(lblMssv);
        infoPanel.add(lblClass);
        infoPanel.add(lblStatus);
        header.add(infoPanel, BorderLayout.WEST);

        // Right actions
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        actionPanel.setOpaque(false);

        JButton btnLogout = new JButton("Đăng xuất");
        btnLogout.setFont(UiUtil.TITLE_FONT);
        btnLogout.setBackground(new Color(220, 53, 69));
        btnLogout.setForeground(Color.WHITE);
        btnLogout.addActionListener(e -> handleLogout());
        actionPanel.add(btnLogout);

        header.add(actionPanel, BorderLayout.EAST);
        return header;
    }

    private void syncEnrollmentsAndSchedule() {
        SwingWorker<List<EnrollmentDetailDto>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<EnrollmentDetailDto> doInBackground() {
                return ClientService.getInstance().getMyEnrollments();
            }

            @Override
            protected void done() {
                try {
                    List<EnrollmentDetailDto> list = get();
                    myEnrollmentsPanel.updateTableData(list);
                    scheduleMatrixPanel.updateSchedule(list);
                } catch (Exception ignored) {
                }
            }
        };
        worker.execute();
    }

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn đăng xuất khỏi hệ thống?",
                "Xác nhận đăng xuất", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            ClientSocketManager.getInstance().removePushListener(this);
            ClientService.getInstance().logout();
            returnToLoginFrame();
        }
    }

    private void returnToLoginFrame() {
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
            dispose();
        });
    }

    // ==========================================
    // ServerPushListener Callbacks
    // ==========================================

    @Override
    public void onCourseUpdated(String courseId, int remainingSlots) {
        // Handled by coursePanel
    }

    @Override
    public void onForceLogout(String message) {
        SwingUtilities.invokeLater(() -> {
            ClientSocketManager.getInstance().removePushListener(this);
            JOptionPane.showMessageDialog(this,
                    message,
                    "Thông báo từ Quản Trị Viên",
                    JOptionPane.WARNING_MESSAGE);
            returnToLoginFrame();
        });
    }

    @Override
    public void onDisconnected(String reason) {
        SwingUtilities.invokeLater(() -> {
            int option = JOptionPane.showConfirmDialog(this,
                    "Mất kết nối với máy chủ: " + reason + "\nBạn có muốn trở về màn hình đăng nhập không?",
                    "Mất kết nối", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
            if (option == JOptionPane.YES_OPTION) {
                returnToLoginFrame();
            }
        });
    }
}
