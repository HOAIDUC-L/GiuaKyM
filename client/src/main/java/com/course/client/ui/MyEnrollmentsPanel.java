package com.course.client.ui;

import com.course.client.service.ClientService;
import com.course.client.service.ScheduleExcelExporter;
import com.course.client.state.ClientState;
import com.course.client.ui.util.UiUtil;
import com.course.common.dto.EnrollmentDetailDto;
import com.course.common.protocol.Response;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel displaying the student's registered courses, with cancellation and Excel export buttons.
 */
public class MyEnrollmentsPanel extends JPanel {
    private final JTable tableEnrollments;
    private final DefaultTableModel tableModel;
    private final JButton btnCancel;
    private final JButton btnExportExcel;
    private final JButton btnRefresh;

    private final List<EnrollmentDetailDto> currentEnrollments = new ArrayList<>();
    private final Runnable onEnrollmentChanged;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public interface ExcelExportHandler {
        void export(List<EnrollmentDetailDto> enrollments, File targetFile);
    }
    private ExcelExportHandler excelExportHandler;

    public MyEnrollmentsPanel(Runnable onEnrollmentChanged) {
        this.onEnrollmentChanged = onEnrollmentChanged;
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        // 1. Top Header
        JPanel topPanel = new JPanel(new BorderLayout());
        JLabel lblTitle = new JLabel("Danh sách học phần đã đăng ký thành công");
        lblTitle.setFont(UiUtil.TITLE_FONT);
        lblTitle.setForeground(UiUtil.PRIMARY_COLOR);
        topPanel.add(lblTitle, BorderLayout.WEST);

        btnRefresh = new JButton("Làm mới danh sách");
        btnRefresh.setPreferredSize(new Dimension(150, 32));
        btnRefresh.addActionListener(e -> loadEnrollments());
        topPanel.add(btnRefresh, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // 2. Table
        String[] columnNames = {
                "Mã lớp HP", "Mã môn", "Tên học phần", "Giảng viên",
                "Lịch học", "Phòng", "Thời gian đăng ký"
        };

        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tableEnrollments = new JTable(tableModel);
        tableEnrollments.setRowHeight(28);
        tableEnrollments.setFont(UiUtil.REGULAR_FONT);
        tableEnrollments.getTableHeader().setFont(UiUtil.TITLE_FONT);
        tableEnrollments.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        tableEnrollments.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        tableEnrollments.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        tableEnrollments.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
        tableEnrollments.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);
        tableEnrollments.getColumnModel().getColumn(6).setCellRenderer(centerRenderer);

        JScrollPane scrollPane = new JScrollPane(tableEnrollments);
        add(scrollPane, BorderLayout.CENTER);

        // 3. Bottom Action Bar
        JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 8));

        btnExportExcel = new JButton("Xuất Thời Khóa Biểu Ra Excel");
        btnExportExcel.setFont(UiUtil.TITLE_FONT);
        btnExportExcel.setBackground(new Color(30, 130, 76));
        btnExportExcel.setForeground(Color.WHITE);
        btnExportExcel.setPreferredSize(new Dimension(240, 38));
        btnExportExcel.addActionListener(this::handleExportExcel);
        actionBar.add(btnExportExcel);

        btnCancel = new JButton("HỦY ĐĂNG KÝ HỌC PHẦN ĐÃ CHỌN");
        btnCancel.setFont(UiUtil.TITLE_FONT);
        btnCancel.setBackground(UiUtil.DANGER_COLOR);
        btnCancel.setForeground(Color.WHITE);
        btnCancel.setPreferredSize(new Dimension(260, 38));
        btnCancel.addActionListener(this::handleCancel);
        actionBar.add(btnCancel);

        add(actionBar, BorderLayout.SOUTH);

        // Initial load
        loadEnrollments();
    }

    public void setExcelExportHandler(ExcelExportHandler handler) {
        this.excelExportHandler = handler;
    }

    public void loadEnrollments() {
        btnRefresh.setEnabled(false);
        SwingWorker<List<EnrollmentDetailDto>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<EnrollmentDetailDto> doInBackground() {
                return ClientService.getInstance().getMyEnrollments();
            }

            @Override
            protected void done() {
                btnRefresh.setEnabled(true);
                try {
                    List<EnrollmentDetailDto> list = get();
                    updateTableData(list);
                } catch (Exception e) {
                    UiUtil.showError(MyEnrollmentsPanel.this, "Không thể tải danh sách đăng ký: " + e.getMessage(), "Lỗi");
                }
            }
        };
        worker.execute();
    }

    public synchronized void updateTableData(List<EnrollmentDetailDto> list) {
        currentEnrollments.clear();
        tableModel.setRowCount(0);

        if (list != null) {
            currentEnrollments.addAll(list);
            for (EnrollmentDetailDto item : list) {
                String regTime = item.getRegisteredAt() != null ? item.getRegisteredAt().format(formatter) : "N/A";
                tableModel.addRow(new Object[]{
                        item.getCourseId(),
                        item.getSubjectCode(),
                        item.getCourseName(),
                        item.getLecturerName(),
                        item.getScheduleDisplay(),
                        item.getRoom(),
                        regTime
                });
            }
        }
    }

    private void handleCancel(ActionEvent event) {
        int selectedRow = tableEnrollments.getSelectedRow();
        if (selectedRow < 0) {
            UiUtil.showWarning(this, "Vui lòng chọn học phần cần hủy đăng ký trong bảng.", "Chưa chọn môn");
            return;
        }

        String courseId = (String) tableModel.getValueAt(selectedRow, 0);
        String courseName = (String) tableModel.getValueAt(selectedRow, 2);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn hủy đăng ký lớp học phần '" + courseName + "' (" + courseId + ") không?",
                "Xác nhận hủy đăng ký", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        btnCancel.setEnabled(false);
        SwingWorker<Response, Void> worker = new SwingWorker<>() {
            @Override
            protected Response doInBackground() {
                return ClientService.getInstance().cancelCourse(courseId);
            }

            @Override
            protected void done() {
                btnCancel.setEnabled(true);
                try {
                    Response response = get();
                    if (response.isSuccess()) {
                        UiUtil.showInfo(MyEnrollmentsPanel.this,
                                "Đã hủy đăng ký thành công lớp học phần: " + courseName,
                                "Hủy thành công");
                        loadEnrollments();
                        if (onEnrollmentChanged != null) {
                            onEnrollmentChanged.run();
                        }
                    } else {
                        UiUtil.showError(MyEnrollmentsPanel.this,
                                "Hủy đăng ký thất bại: " + response.getMessage(),
                                "Lỗi hủy đăng ký");
                    }
                } catch (Exception e) {
                    UiUtil.showError(MyEnrollmentsPanel.this, "Lỗi kết nối máy chủ: " + e.getMessage(), "Lỗi mạng");
                }
            }
        };
        worker.execute();
    }

    private void handleExportExcel(ActionEvent event) {
        if (currentEnrollments.isEmpty()) {
            UiUtil.showWarning(this, "Bạn chưa đăng ký môn học nào để xuất thời khóa biểu.", "Danh sách trống");
            return;
        }

        String studentId = ClientState.getInstance().getCurrentUser() != null ?
                ClientState.getInstance().getCurrentUser().getStudentId() : "STUDENT";

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Lưu file Excel Thời Khóa Biểu");
        fileChooser.setSelectedFile(new File("schedule_" + studentId + "_" + java.time.LocalDate.now() + ".xlsx"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".xlsx")) {
                fileToSave = new File(fileToSave.getParentFile(), fileToSave.getName() + ".xlsx");
            }

            try {
                ScheduleExcelExporter.exportToFile(new ArrayList<>(currentEnrollments), fileToSave);
                UiUtil.showInfo(this, "Xuất file Excel thành công:\n" + fileToSave.getAbsolutePath(), "Xuất thành công");
            } catch (Exception e) {
                UiUtil.showError(this, "Lỗi khi xuất file Excel: " + e.getMessage(), "Lỗi xuất file");
            }
        }
    }

    public List<EnrollmentDetailDto> getCurrentEnrollments() {
        return currentEnrollments;
    }
}
