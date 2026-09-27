package com.course.client.ui;

import com.course.client.service.ClientService;
import com.course.client.ui.util.UiUtil;
import com.course.common.dto.LoginResponseDto;
import com.course.common.protocol.Response;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Swing login frame for student authentication.
 */
public class LoginFrame extends JFrame {
    private final JTextField txtUsername;
    private final JPasswordField txtPassword;
    private final JButton btnLogin;
    private final JLabel lblStatus;

    public LoginFrame() {
        UiUtil.setupLookAndFeel();

        setTitle("Hệ Thống Đăng Ký Học Phần - Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(440, 380);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(new EmptyBorder(25, 30, 25, 30));
        mainPanel.setBackground(Color.WHITE);

        // Header
        JPanel headerPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        headerPanel.setBackground(Color.WHITE);
        JLabel lblTitle = new JLabel("ĐĂNG NHẬP HỆ THỐNG", SwingConstants.CENTER);
        lblTitle.setFont(UiUtil.HEADER_FONT);
        lblTitle.setForeground(UiUtil.PRIMARY_COLOR);

        JLabel lblSubtitle = new JLabel("Cổng thông tin Đăng ký học phần", SwingConstants.CENTER);
        lblSubtitle.setFont(UiUtil.REGULAR_FONT);
        lblSubtitle.setForeground(Color.GRAY);

        headerPanel.add(lblTitle);
        headerPanel.add(lblSubtitle);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Form fields
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 0, 8, 0);

        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel lblUser = new JLabel("Tên đăng nhập / MSSV:");
        lblUser.setFont(UiUtil.TITLE_FONT);
        formPanel.add(lblUser, gbc);

        gbc.gridy = 1;
        txtUsername = new JTextField("sv01", 20);
        txtUsername.setFont(UiUtil.REGULAR_FONT);
        txtUsername.setPreferredSize(new Dimension(280, 36));
        formPanel.add(txtUsername, gbc);

        gbc.gridy = 2;
        JLabel lblPass = new JLabel("Mật khẩu:");
        lblPass.setFont(UiUtil.TITLE_FONT);
        formPanel.add(lblPass, gbc);

        gbc.gridy = 3;
        txtPassword = new JPasswordField("123456", 20);
        txtPassword.setFont(UiUtil.REGULAR_FONT);
        txtPassword.setPreferredSize(new Dimension(280, 36));
        formPanel.add(txtPassword, gbc);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        // Footer & Actions
        JPanel footerPanel = new JPanel(new BorderLayout(10, 10));
        footerPanel.setBackground(Color.WHITE);

        btnLogin = new JButton("ĐĂNG NHẬP");
        btnLogin.setFont(UiUtil.TITLE_FONT);
        btnLogin.setBackground(UiUtil.PRIMARY_COLOR);
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFocusPainted(false);
        btnLogin.setPreferredSize(new Dimension(280, 42));
        btnLogin.addActionListener(this::handleLogin);
        footerPanel.add(btnLogin, BorderLayout.NORTH);

        lblStatus = new JLabel("Sẵn sàng kết nối tới máy chủ", SwingConstants.CENTER);
        lblStatus.setFont(UiUtil.SMALL_FONT);
        lblStatus.setForeground(Color.DARK_GRAY);
        footerPanel.add(lblStatus, BorderLayout.SOUTH);

        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
        getRootPane().setDefaultButton(btnLogin);
    }

    private void handleLogin(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            UiUtil.showWarning(this, "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu.", "Thiếu thông tin");
            return;
        }

        // Disable button during background network request
        btnLogin.setEnabled(false);
        lblStatus.setText("Đang kết nối và xác thực với máy chủ...");
        lblStatus.setForeground(UiUtil.PRIMARY_COLOR);

        // Execute network call on background thread (SwingWorker) to keep UI responsive
        SwingWorker<Response, Void> worker = new SwingWorker<>() {
            @Override
            protected Response doInBackground() {
                return ClientService.getInstance().login(username, password);
            }

            @Override
            protected void done() {
                btnLogin.setEnabled(true);
                try {
                    Response response = get();
                    if (response.isSuccess()) {
                        lblStatus.setText("Đăng nhập thành công! Đang chuyển hướng...");
                        lblStatus.setForeground(UiUtil.SUCCESS_COLOR);

                        LoginResponseDto user = response.getDataAs(LoginResponseDto.class);
                        openMainDashboard(user);
                    } else {
                        lblStatus.setText("Đăng nhập thất bại: " + response.getMessage());
                        lblStatus.setForeground(UiUtil.DANGER_COLOR);
                        UiUtil.showError(LoginFrame.this, response.getMessage(), "Đăng nhập thất bại");
                    }
                } catch (Exception e) {
                    lblStatus.setText("Lỗi kết nối máy chủ");
                    lblStatus.setForeground(UiUtil.DANGER_COLOR);
                    UiUtil.showError(LoginFrame.this, "Không thể kết nối đến máy chủ: " + e.getMessage(), "Lỗi mạng");
                }
            }
        };
        worker.execute();
    }

    private void openMainDashboard(LoginResponseDto user) {
        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = new MainFrame(user);
            mainFrame.setVisible(true);
            dispose();
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LoginFrame frame = new LoginFrame();
            frame.setVisible(true);
        });
    }
}
