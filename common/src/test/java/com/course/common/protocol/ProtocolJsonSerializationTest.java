package com.course.common.protocol;

import com.course.common.dto.LoginRequestDto;
import com.course.common.dto.LoginResponseDto;
import com.course.common.model.Role;
import com.course.common.util.JsonUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProtocolJsonSerializationTest {

    @Test
    @DisplayName("Request serialization and deserialization with generic DTO payload")
    void testRequestSerialization() {
        LoginRequestDto dto = new LoginRequestDto("sv01", "123456");
        Request request = Request.of(Action.LOGIN, "REQ-999", dto);

        String json = JsonUtil.toJson(request);
        assertNotNull(json);
        assertTrue(json.contains("\"action\":\"LOGIN\""));
        assertTrue(json.contains("\"requestId\":\"REQ-999\""));
        assertTrue(json.contains("\"username\":\"sv01\""));

        Request parsed = JsonUtil.fromJson(json, Request.class);
        assertNotNull(parsed);
        assertEquals(Action.LOGIN, parsed.getAction());
        assertEquals("REQ-999", parsed.getRequestId());

        LoginRequestDto parsedDto = parsed.getDataAs(LoginRequestDto.class);
        assertNotNull(parsedDto);
        assertEquals("sv01", parsedDto.getUsername());
        assertEquals("123456", parsedDto.getPassword());
    }

    @Test
    @DisplayName("Response success and error serialization with ErrorCode")
    void testResponseSerialization() {
        // Success
        LoginResponseDto loginRes = new LoginResponseDto("SV001", "sv01", "Nguyen Van A", "sv01@student.edu.vn", "CNTT1", Role.STUDENT);
        Response successRes = Response.success(Action.LOGIN_RESPONSE, "REQ-100", "Login successful", loginRes);

        String successJson = JsonUtil.toJson(successRes);
        assertTrue(successJson.contains("\"success\":true"));

        Response parsedSuccess = JsonUtil.fromJson(successJson, Response.class);
        assertTrue(parsedSuccess.isSuccess());
        LoginResponseDto parsedUser = parsedSuccess.getDataAs(LoginResponseDto.class);
        assertEquals("SV001", parsedUser.getStudentId());

        // Error
        Response errorRes = Response.error(Action.REGISTER_COURSE_RESPONSE, "REQ-101", ErrorCode.COURSE_FULL, "Lớp đã hết chỗ");
        String errorJson = JsonUtil.toJson(errorRes);
        assertTrue(errorJson.contains("\"success\":false"));
        assertTrue(errorJson.contains("\"errorCode\":\"COURSE_FULL\""));

        Response parsedError = JsonUtil.fromJson(errorJson, Response.class);
        assertFalse(parsedError.isSuccess());
        assertEquals(ErrorCode.COURSE_FULL, parsedError.getErrorCode());
    }
}
