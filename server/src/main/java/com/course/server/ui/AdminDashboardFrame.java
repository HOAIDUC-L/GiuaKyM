package com.course.server.ui;

import com.course.common.dto.BlockUserRequestDto;
import com.course.common.dto.ClientSessionDto;
import com.course.common.protocol.Response;
import com.course.server.ServerContext;
import com.course.server.config.ServerConfig;
import com.course.server.network.TcpServer;
import com.course.server.session.SessionManager;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Logger;

/**
 * Swing Admin Dashboard for managing the TCP Server, connected clients, and user accounts.
 */
public class AdminDashboardFrame extends JFrame {
    private static final Logger ROOT_LOGGER = Logger.getLogger("com.course.server");

    // Styling constants
    private static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 18);
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font FONT_MONO = new Font("Consolas", Font.PLAIN, 12);

    private static final Color COLOR_PRIMARY = new Color(24, 119, 242);
    private static final Color COLOR_SUCCESS = new Color(46, 125, 50);
    private static final Color COLOR_DANGER = new Color(220, 53, 69);
    private static final Color COLOR_WARNING = new Color(239, 108, 0);

    // Server components
    private TcpServer tcpServer;
    private final ServerConfig config = ServerConfig.getInstance();

    // UI Components
    private JButton btnStartServer;
    private JButton btnStopServer;
    private JTextField txtPort;
    private JLabel lblStatusBadge;
    private JLabel lblOnlineBadge;

    private JTable tableClients;
    private DefaultTableModel tableModelClients;
    private JButton btnKickClient;
    private JButton btnRefreshClients;

    private JTextField txtAccountIdentifier;
    private JButton btnBlockAccount;
    private JButton btnUnblockAccount;

    private JTextArea txtLogArea;
    private JButton btnClearLog;

    private Timer autoRefreshTimer;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    public AdminDashboardFrame() {
        setupLookAndFeel();

        setTitle("Course Registration System - Server Control & Admin Dashboard");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1150, 750);
        setMinimumSize(new Dimension(980, 650));
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleExit();
            }
        });

        // Initialize ServerContext with default instance
        ServerContext.getInstance().initialize(null);

        // Build UI Layout
        JPanel mainContainer = new JPanel(new BorderLayout(10, 10));
        mainContainer.setBorder(new EmptyBorder(12, 12, 12, 12));
        setContentPane(mainContainer);

        // 1. Top Section: Header & Server Controls
        mainContainer.add(createTopControlPanel(), BorderLayout.NORTH);

        // 2. Center Section: Online Clients & Account Management
        JSplitPane centerSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        centerSplitPane.setResizeWeight(0.60);
        centerSplitPane.setContinuousLayout(true);

        centerSplitPane.setTopComponent(createClientsPanel());
        centerSplitPane.setBottomComponent(createLogAndAccountPanel());

        mainContainer.add(centerSplitPane, BorderLayout.CENTER);

        // Setup Log Appender
        ROOT_LOGGER.addHandler(new ServerLogAppender(txtLogArea));

        // Setup Auto-refresh timer for online clients
        autoRefreshTimer = new Timer(2000, e -> refreshOnlineClients());
        autoRefreshTimer.start();

        // Auto-start server on launch if configured
        startServerAction();
    }

    private void setupLookAndFeel() {
        try {
            FlatLightLaf.setup();
        } catch (Throwable t) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
        }
        UIManager.put("defaultFont", FONT_REGULAR);
    }

    private JPanel createTopControlPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 10));
        panel.setBorder(new CompoundBorder(
                new LineBorder(new Color(220, 224, 230), 1),
                new EmptyBorder(10, 15, 10, 15)
        ));
        panel.setBackground(Color.WHITE);

        // Title
        JLabel lblTitle = new JLabel("HỆ THỐNG ĐĂNG KÝ HỌC PHẦN - MÁY CHỦ QUẢN TRỊ (ADMIN)");
        lblTitle.setFont(FONT_HEADER);
        lblTitle.setForeground(COLOR_PRIMARY);
        panel.add(lblTitle, BorderLayout.WEST);

        // Controls
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        controls.setOpaque(false);

        controls.add(new JLabel("Cổng (Port):"));
        txtPort = new JTextField(String.valueOf(config.getServerPort()), 5);
        txtPort.setFont(FONT_REGULAR);
        txtPort.setHorizontalAlignment(SwingConstants.CENTER);
        controls.add(txtPort);

        btnStartServer = new JButton("BẮT ĐẦU (START)");
        btnStartServer.setFont(FONT_TITLE);
        btnStartServer.setBackground(COLOR_SUCCESS);
        btnStartServer.setForeground(Color.WHITE);
        btnStartServer.addActionListener(e -> startServerAction());
        controls.add(btnStartServer);

        btnStopServer = new JButton("DỪNG (STOP)");
        btnStopServer.setFont(FONT_TITLE);
        btnStopServer.setBackground(COLOR_DANGER);
        btnStopServer.setForeground(Color.WHITE);
        btnStopServer.setEnabled(false);
        btnStopServer.addActionListener(e -> stopServerAction());
        controls.add(btnStopServer);

        lblStatusBadge = new JLabel("  STOPPED  ");
        lblStatusBadge.setFont(FONT_TITLE);
        lblStatusBadge.setOpaque(true);
        lblStatusBadge.setBackground(COLOR_DANGER);
        lblStatusBadge.setForeground(Color.WHITE);
        lblStatusBadge.setBorder(new EmptyBorder(4, 8, 4, 8));
        controls.add(lblStatusBadge);

        lblOnlineBadge = new JLabel("Online: 0");
        lblOnlineBadge.setFont(FONT_TITLE);
        lblOnlineBadge.setForeground(COLOR_PRIMARY);
        controls.add(lblOnlineBadge);

        panel.add(controls, BorderLayout.EAST);
        return panel;
    }

    private JPanel createClientsPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 205, 210)),
                "Danh Sách Client Đang Trực Tuyến (Online Clients)",
                TitledBorder.LEFT, TitledBorder.TOP, FONT_TITLE, COLOR_PRIMARY));

        String[] columns = {
                "Session ID", "IP", "Port", "MSSV", "Username", "Họ Tên",
                "Email", "Quyền", "Thời Gian Login", "Online", "Trạng Thái"
        };

        tableModelClients = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tableClients = new JTable(tableModelClients);
        tableClients.setRowHeight(26);
        tableClients.setFont(FONT_REGULAR);
        tableClients.getTableHeader().setFont(FONT_TITLE);
        tableClients.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        tableClients.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        tableClients.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        tableClients.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        tableClients.getColumnModel().getColumn(7).setCellRenderer(centerRenderer);
        tableClients.getColumnModel().getColumn(8).setCellRenderer(centerRenderer);
        tableClients.getColumnModel().getColumn(9).setCellRenderer(centerRenderer);
        tableClients.getColumnModel().getColumn(10).setCellRenderer(centerRenderer);

        JScrollPane scrollPane = new JScrollPane(tableClients);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Action Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        btnRefreshClients = new JButton("Làm Mới Danh Sách");
        btnRefreshClients.addActionListener(e -> refreshOnlineClients());
        toolbar.add(btnRefreshClients);

        btnKickClient = new JButton("KICK CLIENT ĐÃ CHỌN");
        btnKickClient.setFont(FONT_TITLE);
        btnKickClient.setBackground(COLOR_DANGER);
        btnKickClient.setForeground(Color.WHITE);
        btnKickClient.addActionListener(this::handleKickClient);
        toolbar.add(btnKickClient);

        panel.add(toolbar, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createLogAndAccountPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));

        // Account management bar on top
        JPanel accountPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        accountPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 205, 210)),
                "Quản Lý Tài Khoản Người Dùng",
                TitledBorder.LEFT, TitledBorder.TOP, FONT_TITLE, COLOR_PRIMARY));

        accountPanel.add(new JLabel("MSSV / Email / Username:"));
        txtAccountIdentifier = new JTextField(15);
        txtAccountIdentifier.setFont(FONT_REGULAR);
        accountPanel.add(txtAccountIdentifier);

        btnBlockAccount = new JButton("KHÓA TÀI KHOẢN (BLOCK)");
        btnBlockAccount.setFont(FONT_TITLE);
        btnBlockAccount.setBackground(COLOR_WARNING);
        btnBlockAccount.setForeground(Color.WHITE);
        btnBlockAccount.addActionListener(e -> handleBlockAccount(true));
        accountPanel.add(btnBlockAccount);

        btnUnblockAccount = new JButton("MỞ KHÓA TÀI KHOẢN (UNBLOCK)");
        btnUnblockAccount.setFont(FONT_TITLE);
        btnUnblockAccount.setBackground(COLOR_SUCCESS);
        btnUnblockAccount.setForeground(Color.WHITE);
        btnUnblockAccount.addActionListener(e -> handleBlockAccount(false));
        accountPanel.add(btnUnblockAccount);

        panel.add(accountPanel, BorderLayout.NORTH);

        // Console Log Viewer
        JPanel logPanel = new JPanel(new BorderLayout(5, 5));
        logPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 205, 210)),
                "Nhật Ký Máy Chủ (Server Event Log)",
                TitledBorder.LEFT, TitledBorder.TOP, FONT_TITLE, COLOR_PRIMARY));

        txtLogArea = new JTextArea();
        txtLogArea.setEditable(false);
        txtLogArea.setFont(FONT_MONO);
        txtLogArea.setBackground(new Color(24, 24, 24));
        txtLogArea.setForeground(new Color(220, 220, 220));
        txtLogArea.setCaretColor(Color.WHITE);

        JScrollPane logScroll = new JScrollPane(txtLogArea);
        logPanel.add(logScroll, BorderLayout.CENTER);

        JPanel logActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        btnClearLog = new JButton("Xóa Log");
        btnClearLog.addActionListener(e -> txtLogArea.setText(""));
        logActions.add(btnClearLog);
        logPanel.add(logActions, BorderLayout.SOUTH);

        panel.add(logPanel, BorderLayout.CENTER);
        return panel;
    }

    // ==========================================
    // Server Control Logic
    // ==========================================

    private void startServerAction() {
        int port;
        try {
            port = Integer.parseInt(txtPort.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Cổng phải là số nguyên hợp lệ.", "Lỗi cổng", JOptionPane.ERROR_MESSAGE);
            return;
        }

        btnStartServer.setEnabled(false);
        txtPort.setEnabled(false);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                tcpServer = new TcpServer(port, config.getServerMaxClients(), config.getHeartbeatTimeoutMs());
                ServerContext.getInstance().initialize(tcpServer);
                tcpServer.start();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    lblStatusBadge.setText("  RUNNING  ");
                    lblStatusBadge.setBackground(COLOR_SUCCESS);
                    btnStopServer.setEnabled(true);
                } catch (Exception e) {
                    lblStatusBadge.setText("  ERROR  ");
                    lblStatusBadge.setBackground(COLOR_DANGER);
                    btnStartServer.setEnabled(true);
                    txtPort.setEnabled(true);
                    JOptionPane.showMessageDialog(AdminDashboardFrame.this,
                            "Không thể khởi động máy chủ: " + e.getMessage(), "Lỗi khởi động", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void stopServerAction() {
        btnStopServer.setEnabled(false);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                if (tcpServer != null) {
                    tcpServer.stop();
                }
                return null;
            }

            @Override
            protected void done() {
                lblStatusBadge.setText("  STOPPED  ");
                lblStatusBadge.setBackground(COLOR_DANGER);
                btnStartServer.setEnabled(true);
                txtPort.setEnabled(true);
                refreshOnlineClients();
            }
        };
        worker.execute();
    }

    private void refreshOnlineClients() {
        List<ClientSessionDto> sessions = SessionManager.getInstance().getAllSessionDtos();
        lblOnlineBadge.setText("Online: " + sessions.size());

        // Update table smoothly
        int selectedRow = tableClients.getSelectedRow();
        String selectedSessionId = (selectedRow >= 0 && selectedRow < tableModelClients.getRowCount()) ?
                (String) tableModelClients.getValueAt(selectedRow, 0) : null;

        tableModelClients.setRowCount(0);
        int newSelectedRow = -1;

        for (int i = 0; i < sessions.size(); i++) {
            ClientSessionDto s = sessions.get(i);
            String loginStr = s.getLoginTime() != null ? s.getLoginTime().format(timeFormatter) : "N/A";
            tableModelClients.addRow(new Object[]{
                    s.getSessionId(),
                    s.getClientIp(),
                    s.getClientPort(),
                    s.getStudentId(),
                    s.getUsername(),
                    s.getFullName(),
                    s.getEmail(),
                    s.getRole(),
                    loginStr,
                    s.getFormattedDuration(),
                    s.getStatus()
            });

            if (selectedSessionId != null && selectedSessionId.equals(s.getSessionId())) {
                newSelectedRow = i;
            }
        }

        if (newSelectedRow >= 0) {
            tableClients.setRowSelectionInterval(newSelectedRow, newSelectedRow);
        }
    }

    private void handleKickClient(ActionEvent event) {
        int selectedRow = tableClients.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một client trong bảng để ngắt kết nối.", "Chưa chọn client", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sessionId = (String) tableModelClients.getValueAt(selectedRow, 0);
        String studentId = (String) tableModelClients.getValueAt(selectedRow, 3);
        String username = (String) tableModelClients.getValueAt(selectedRow, 4);

        int confirm = JOptionPane.showConfirmDialog(this,
                String.format("Bạn có chắc chắn muốn đá Client (Session: %s, User: %s / %s) ra khỏi hệ thống?",
                        sessionId, studentId, username),
                "Xác nhận Kick Client", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            boolean kicked = SessionManager.getInstance().kickSession(sessionId, "Bạn đã bị quản trị viên đá khỏi hệ thống.");
            if (kicked) {
                JOptionPane.showMessageDialog(this, "Đã ngắt kết nối client thành công.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                refreshOnlineClients();
            } else {
                JOptionPane.showMessageDialog(this, "Không thể kick client (có thể client đã ngắt kết nối).", "Thất bại", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleBlockAccount(boolean block) {
        String identifier = txtAccountIdentifier.getText().trim();
        if (identifier.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập MSSV, Username hoặc Email cần xử lý.", "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String actionName = block ? "KHÓA" : "MỞ KHÓA";
        int confirm = JOptionPane.showConfirmDialog(this,
                String.format("Bạn có chắc chắn muốn %s tài khoản '%s' không?", actionName, identifier),
                "Xác nhận " + actionName, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        BlockUserRequestDto dto = new BlockUserRequestDto(identifier, block);
        Response response = ServerContext.getInstance().getAdminService().blockUser(dto, null);

        if (response.isSuccess()) {
            JOptionPane.showMessageDialog(this, response.getMessage(), "Thành công", JOptionPane.INFORMATION_MESSAGE);
            txtAccountIdentifier.setText("");
            refreshOnlineClients();
        } else {
            JOptionPane.showMessageDialog(this, "Thao tác thất bại: " + response.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleExit() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Đóng bảng điều khiển sẽ dừng toàn bộ máy chủ và ngắt kết nối tất cả client.\nBạn có chắc chắn muốn thoát?",
                "Xác nhận thoát", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            if (autoRefreshTimer != null) {
                autoRefreshTimer.stop();
            }
            if (tcpServer != null && tcpServer.isRunning()) {
                tcpServer.stop();
            }
            dispose();
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AdminDashboardFrame frame = new AdminDashboardFrame();
            frame.setVisible(true);
        });
    }
}
