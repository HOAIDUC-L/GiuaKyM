package com.course.client.ui;

import com.course.client.ui.util.UiUtil;
import com.course.common.constants.AppConstants;
import com.course.common.dto.EnrollmentDetailDto;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Visual timetable matrix panel displaying a 10-period x 7-day schedule grid.
 */
public class ScheduleMatrixPanel extends JPanel {
    private final JTable tableSchedule;
    private final DefaultTableModel tableModel;

    public ScheduleMatrixPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        // Top title
        JLabel lblTitle = new JLabel("Thời Khóa Biểu Tuần (Ma trận 10 Tiết x 7 Ngày)");
        lblTitle.setFont(UiUtil.TITLE_FONT);
        lblTitle.setForeground(UiUtil.PRIMARY_COLOR);
        add(lblTitle, BorderLayout.NORTH);

        // Column headers: Tiết, Thứ 2, Thứ 3, Thứ 4, Thứ 5, Thứ 6, Thứ 7, Chủ Nhật
        String[] columns = {
                "Tiết học / Giờ",
                "Thứ 2 (Mon)",
                "Thứ 3 (Tue)",
                "Thứ 4 (Wed)",
                "Thứ 5 (Thu)",
                "Thứ 6 (Fri)",
                "Thứ 7 (Sat)",
                "Chủ Nhật (Sun)"
        };

        tableModel = new DefaultTableModel(columns, AppConstants.MAX_PERIOD) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // Populate row headers (Period 1 to 10 with time)
        for (int p = 1; p <= AppConstants.MAX_PERIOD; p++) {
            tableModel.setValueAt("<html><center><b>Tiết " + p + "</b><br><small>" + AppConstants.getPeriodTiming(p) + "</small></center></html>", p - 1, 0);
        }

        tableSchedule = new JTable(tableModel);
        tableSchedule.setRowHeight(54);
        tableSchedule.getTableHeader().setFont(UiUtil.TITLE_FONT);
        tableSchedule.getTableHeader().setPreferredSize(new Dimension(100, 36));
        tableSchedule.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableSchedule.setCellSelectionEnabled(false);

        // Column 0 renderer (Periods)
        DefaultTableCellRenderer periodRenderer = new DefaultTableCellRenderer();
        periodRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        periodRenderer.setBackground(new Color(245, 247, 250));
        tableSchedule.getColumnModel().getColumn(0).setCellRenderer(periodRenderer);
        tableSchedule.getColumnModel().getColumn(0).setPreferredWidth(140);

        // Course cell renderer for day columns
        ScheduleCellRenderer courseRenderer = new ScheduleCellRenderer();
        for (int c = 1; c <= 7; c++) {
            tableSchedule.getColumnModel().getColumn(c).setCellRenderer(courseRenderer);
            tableSchedule.getColumnModel().getColumn(c).setPreferredWidth(160);
        }

        JScrollPane scrollPane = new JScrollPane(tableSchedule);
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Updates the matrix using current enrolled courses.
     */
    public synchronized void updateSchedule(List<EnrollmentDetailDto> enrollments) {
        // Clear previous schedule cells (columns 1 to 7)
        for (int r = 0; r < AppConstants.MAX_PERIOD; r++) {
            for (int c = 1; c <= 7; c++) {
                tableModel.setValueAt(null, r, c);
            }
        }

        if (enrollments != null) {
            for (EnrollmentDetailDto item : enrollments) {
                int col = item.getDayOfWeek() - 1; // Day 2 (Mon) -> col 1, Day 8 (Sun) -> col 7
                if (col >= 1 && col <= 7) {
                    for (int p = item.getStartPeriod(); p <= item.getEndPeriod(); p++) {
                        if (p >= 1 && p <= AppConstants.MAX_PERIOD) {
                            String cellHtml = String.format(
                                    "<html><center><b style='color:#1565C0;'>%s</b><br>%s<br><span style='color:#2E7D32;'>P: %s</span></center></html>",
                                    item.getSubjectCode(),
                                    truncateText(item.getCourseName(), 20),
                                    item.getRoom()
                            );
                            tableModel.setValueAt(cellHtml, p - 1, col);
                        }
                    }
                }
            }
        }
        tableModel.fireTableDataChanged();
    }

    private String truncateText(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength - 3) + "..." : text;
    }

    private static class ScheduleCellRenderer extends DefaultTableCellRenderer {
        private static final Color OCCUPIED_BG = new Color(227, 242, 253); // Light soft blue
        private static final Color EMPTY_BG = Color.WHITE;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(SwingConstants.CENTER);

            if (value != null && !value.toString().isBlank()) {
                c.setBackground(OCCUPIED_BG);
            } else {
                c.setBackground(EMPTY_BG);
                setText("");
            }
            return c;
        }
    }
}
