package com.course.client.service;

import com.course.client.state.ClientState;
import com.course.common.constants.AppConstants;
import com.course.common.dto.EnrollmentDetailDto;
import com.course.common.dto.LoginResponseDto;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Generates formatted Excel workbooks (.xlsx) with two sheets:
 * 1. Timetable Matrix (10 periods x 7 days)
 * 2. Detailed list of registered courses.
 */
public class ScheduleExcelExporter {
    private static final Logger LOGGER = Logger.getLogger(ScheduleExcelExporter.class.getName());

    public static void exportToFile(List<EnrollmentDetailDto> enrollments, File targetFile) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            LoginResponseDto student = ClientState.getInstance().getCurrentUser();
            String studentName = student != null ? student.getFullName() : "N/A";
            String studentId = student != null ? student.getStudentId() : "N/A";
            String className = student != null && student.getClassName() != null ? student.getClassName() : "N/A";

            // Create styles
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle subtitleStyle = createSubtitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle periodHeaderStyle = createPeriodHeaderStyle(workbook);
            CellStyle cellStyle = createBorderedCellStyle(workbook);
            CellStyle occupiedCellStyle = createOccupiedCellStyle(workbook);
            CellStyle centerStyle = createCenterCellStyle(workbook);

            // ==========================================
            // SHEET 1: Thời Khóa Biểu Tuần (Ma trận)
            // ==========================================
            Sheet sheet1 = workbook.createSheet("Thời Khóa Biểu");
            sheet1.setDisplayGridlines(true);

            // Row 0: Main Title
            Row titleRow = sheet1.createRow(0);
            titleRow.setHeightInPoints(32);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("THỜI KHÓA BIỂU HỌC TẬP SINH VIÊN");
            titleCell.setCellStyle(titleStyle);
            sheet1.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));

            // Row 1: Student Information
            Row infoRow = sheet1.createRow(1);
            infoRow.setHeightInPoints(22);
            Cell infoCell = infoRow.createCell(0);
            String infoText = String.format("Sinh viên: %s  |  MSSV: %s  |  Lớp: %s  |  Ngày xuất: %s",
                    studentName, studentId, className, LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            infoCell.setCellValue(infoText);
            infoCell.setCellStyle(subtitleStyle);
            sheet1.addMergedRegion(new CellRangeAddress(1, 1, 0, 7));

            // Row 2: Empty spacer
            sheet1.createRow(2).setHeightInPoints(10);

            // Row 3: Timetable Table Headers
            Row headerRow = sheet1.createRow(3);
            headerRow.setHeightInPoints(28);
            String[] headers = {
                    "Tiết học / Giờ", "Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "Chủ Nhật"
            };
            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            // Rows 4 to 13: 10 Periods
            Row[] periodRows = new Row[AppConstants.MAX_PERIOD];
            for (int p = 1; p <= AppConstants.MAX_PERIOD; p++) {
                int rowIndex = 3 + p;
                Row r = sheet1.createRow(rowIndex);
                r.setHeightInPoints(45);
                periodRows[p - 1] = r;

                // Column 0: Period header
                Cell periodCell = r.createCell(0);
                periodCell.setCellValue("Tiết " + p + "\n" + AppConstants.getPeriodTiming(p));
                periodCell.setCellStyle(periodHeaderStyle);

                // Columns 1 to 7: Empty default bordered cells
                for (int d = 1; d <= 7; d++) {
                    Cell c = r.createCell(d);
                    c.setCellValue("");
                    c.setCellStyle(cellStyle);
                }
            }

            // Fill enrolled courses into matrix
            if (enrollments != null) {
                for (EnrollmentDetailDto item : enrollments) {
                    int col = item.getDayOfWeek() - 1; // 2 (Mon) -> col 1, ..., 8 (Sun) -> col 7
                    if (col >= 1 && col <= 7) {
                        for (int p = item.getStartPeriod(); p <= item.getEndPeriod(); p++) {
                            if (p >= 1 && p <= AppConstants.MAX_PERIOD) {
                                Cell c = periodRows[p - 1].getCell(col);
                                String cellText = String.format("%s - %s\nPhòng: %s\nGV: %s",
                                        item.getSubjectCode(), item.getCourseName(), item.getRoom(), item.getLecturerName());
                                c.setCellValue(cellText);
                                c.setCellStyle(occupiedCellStyle);
                            }
                        }
                    }
                }
            }

            // Auto-size columns for Sheet 1
            for (int i = 0; i < 8; i++) {
                sheet1.setColumnWidth(i, i == 0 ? 18 * 256 : 28 * 256);
            }

            // ==========================================
            // SHEET 2: Danh Sách Học Phần Đã Đăng Ký
            // ==========================================
            Sheet sheet2 = workbook.createSheet("Học Phần Đã Đăng Ký");
            sheet2.setDisplayGridlines(true);

            // Row 0: Title
            Row titleRow2 = sheet2.createRow(0);
            titleRow2.setHeightInPoints(30);
            Cell titleCell2 = titleRow2.createCell(0);
            titleCell2.setCellValue("DANH SÁCH HỌC PHẦN ĐÃ ĐĂNG KÝ");
            titleCell2.setCellStyle(titleStyle);
            sheet2.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));

            // Row 1: Subtitle
            Row infoRow2 = sheet2.createRow(1);
            infoRow2.setHeightInPoints(20);
            Cell infoCell2 = infoRow2.createCell(0);
            infoCell2.setCellValue(infoText);
            infoCell2.setCellStyle(subtitleStyle);
            sheet2.addMergedRegion(new CellRangeAddress(1, 1, 0, 7));

            // Row 2: Spacer
            sheet2.createRow(2).setHeightInPoints(8);

            // Row 3: Headers
            Row headerRow2 = sheet2.createRow(3);
            headerRow2.setHeightInPoints(26);
            String[] listHeaders = {
                    "STT", "Mã Lớp HP", "Mã Môn", "Tên Học Phần",
                    "Giảng Viên", "Lịch Học", "Phòng Học", "Ngày Đăng Ký"
            };
            for (int i = 0; i < listHeaders.length; i++) {
                Cell c = headerRow2.createCell(i);
                c.setCellValue(listHeaders[i]);
                c.setCellStyle(headerStyle);
            }

            // Rows 4+: Registered courses list
            DateTimeFormatter regFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            if (enrollments != null && !enrollments.isEmpty()) {
                int stt = 1;
                int rowIdx = 4;
                for (EnrollmentDetailDto item : enrollments) {
                    Row r = sheet2.createRow(rowIdx++);
                    r.setHeightInPoints(22);

                    Cell c0 = r.createCell(0);
                    c0.setCellValue(stt++);
                    c0.setCellStyle(centerStyle);

                    Cell c1 = r.createCell(1);
                    c1.setCellValue(item.getCourseId());
                    c1.setCellStyle(centerStyle);

                    Cell c2 = r.createCell(2);
                    c2.setCellValue(item.getSubjectCode());
                    c2.setCellStyle(centerStyle);

                    Cell c3 = r.createCell(3);
                    c3.setCellValue(item.getCourseName());
                    c3.setCellStyle(cellStyle);

                    Cell c4 = r.createCell(4);
                    c4.setCellValue(item.getLecturerName());
                    c4.setCellStyle(cellStyle);

                    Cell c5 = r.createCell(5);
                    c5.setCellValue(item.getScheduleDisplay());
                    c5.setCellStyle(centerStyle);

                    Cell c6 = r.createCell(6);
                    c6.setCellValue(item.getRoom());
                    c6.setCellStyle(centerStyle);

                    Cell c7 = r.createCell(7);
                    c7.setCellValue(item.getRegisteredAt() != null ? item.getRegisteredAt().format(regFormatter) : "N/A");
                    c7.setCellStyle(centerStyle);
                }
            }

            // Auto-size columns for Sheet 2
            for (int i = 0; i < listHeaders.length; i++) {
                sheet2.autoSizeColumn(i);
                sheet2.setColumnWidth(i, Math.max(sheet2.getColumnWidth(i) + 1024, 12 * 256));
            }

            // Write to file
            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                workbook.write(fos);
            }
            LOGGER.info("Excel timetable exported successfully to: " + targetFile.getAbsolutePath());
        }
    }

    private static CellStyle createTitleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Segoe UI");
        font.setFontHeightInPoints((short) 16);
        font.setBold(true);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private static CellStyle createSubtitleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Segoe UI");
        font.setFontHeightInPoints((short) 10);
        font.setItalic(true);
        font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private static CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Segoe UI");
        font.setFontHeightInPoints((short) 11);
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        style.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        setBorders(style, BorderStyle.THIN);
        return style;
    }

    private static CellStyle createPeriodHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Segoe UI");
        font.setFontHeightInPoints((short) 9);
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);

        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        setBorders(style, BorderStyle.THIN);
        return style;
    }

    private static CellStyle createBorderedCellStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Segoe UI");
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        setBorders(style, BorderStyle.THIN);
        return style;
    }

    private static CellStyle createCenterCellStyle(Workbook wb) {
        CellStyle style = createBorderedCellStyle(wb);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private static CellStyle createOccupiedCellStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Segoe UI");
        font.setFontHeightInPoints((short) 9);
        font.setBold(true);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);

        style.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        setBorders(style, BorderStyle.MEDIUM);
        return style;
    }

    private static void setBorders(CellStyle style, BorderStyle borderStyle) {
        style.setBorderTop(borderStyle);
        style.setBorderBottom(borderStyle);
        style.setBorderLeft(borderStyle);
        style.setBorderRight(borderStyle);
    }
}
