package com.course.client.service;

import com.course.client.state.ClientState;
import com.course.common.dto.EnrollmentDetailDto;
import com.course.common.dto.LoginResponseDto;
import com.course.common.model.Role;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScheduleExcelExporterTest {

    @Test
    @DisplayName("Verify Apache POI generates valid 2-sheet Excel workbook with schedule and courses")
    void testExportToFile() throws Exception {
        LoginResponseDto student = new LoginResponseDto("SV001", "sv01", "Nguyen Van A", "sv01@student.ptit.edu.vn", "CNTT1", Role.STUDENT);
        ClientState.getInstance().setCurrentUser(student);

        List<EnrollmentDetailDto> list = new ArrayList<>();
        list.add(new EnrollmentDetailDto(
                1L, "SV001", "INT101_01", "INT101", "Lập trình Mạng",
                "TS. Nguyễn Văn D", 2, 1, 3, "301-A2", LocalDateTime.now()
        ));
        list.add(new EnrollmentDetailDto(
                2L, "SV001", "INT104_01", "INT104", "Cơ sở Dữ liệu Phân tán",
                "TS. Đỗ Mạnh H", 3, 1, 3, "101-A1", LocalDateTime.now()
        ));

        File tempFile = File.createTempFile("test_schedule_", ".xlsx");
        tempFile.deleteOnExit();

        ScheduleExcelExporter.exportToFile(list, tempFile);

        assertTrue(tempFile.exists(), "Excel file must be created on disk");
        assertTrue(tempFile.length() > 0, "Excel file must not be empty");

        // Read file back using Apache POI to verify contents and sheet structure
        try (FileInputStream fis = new FileInputStream(tempFile);
             Workbook workbook = WorkbookFactory.create(fis)) {
            assertEquals(2, workbook.getNumberOfSheets(), "Workbook must contain exactly 2 sheets");
            assertEquals("Thời Khóa Biểu", workbook.getSheetName(0));
            assertEquals("Học Phần Đã Đăng Ký", workbook.getSheetName(1));

            // Verify Sheet 1 title contains student text
            String titleText = workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue();
            assertTrue(titleText.contains("THỜI KHÓA BIỂU"));

            // Verify Sheet 2 has rows
            assertTrue(workbook.getSheetAt(1).getPhysicalNumberOfRows() >= 5);
        }
    }
}
