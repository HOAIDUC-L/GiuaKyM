package com.course.common.protocol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.course.common.util.JsonUtil;

import java.io.Serializable;

/**
 * TCP Response and Server-Push message framing structure.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Response implements Serializable {
    private static final long serialVersionUID = 1L;

    private Action action;
    private String requestId;
    private boolean success;
    private ErrorCode errorCode;
    private String message;
    private JsonNode data;

    public Response() {
    }

    public Response(Action action, String requestId, boolean success, ErrorCode errorCode, String message, JsonNode data) {
        this.action = action;
        this.requestId = requestId;
        this.success = success;
        this.errorCode = errorCode;
        this.message = message;
        this.data = data;
    }

    // Factory methods for Success
    public static Response success(Action action, String requestId, String message, Object dataObj) {
        JsonNode node = dataObj != null ? JsonUtil.getObjectMapper().valueToTree(dataObj) : null;
        return new Response(action, requestId, true, null, message, node);
    }

    public static Response success(Action action, String requestId, String message) {
        return success(action, requestId, message, null);
    }

    // Factory methods for Error
    public static Response error(Action action, String requestId, ErrorCode errorCode, String message) {
        String msg = (message != null && !message.isBlank()) ? message : (errorCode != null ? errorCode.getDefaultMessage() : "Lỗi không xác định");
        return new Response(action, requestId, false, errorCode, msg, null);
    }

    public static Response error(Action action, String requestId, ErrorCode errorCode) {
        return error(action, requestId, errorCode, errorCode.getDefaultMessage());
    }

    // Factory methods for Server Push / Broadcast
    public static Response push(Action action, Object dataObj) {
        JsonNode node = dataObj != null ? JsonUtil.getObjectMapper().valueToTree(dataObj) : null;
        return new Response(action, null, true, null, null, node);
    }

    public static Response forceLogout(String message) {
        return new Response(Action.FORCE_LOGOUT, null, false, ErrorCode.ACCOUNT_DISABLED, message, null);
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public JsonNode getData() {
        return data;
    }

    public void setData(JsonNode data) {
        this.data = data;
    }

    public <T> T getDataAs(Class<T> clazz) {
        if (data == null || data.isNull()) {
            return null;
        }
        return JsonUtil.getObjectMapper().convertValue(data, clazz);
    }

    @Override
    public String toString() {
        return "Response{" +
                "action=" + action +
                ", requestId='" + requestId + '\'' +
                ", success=" + success +
                ", errorCode=" + errorCode +
                ", message='" + message + '\'' +
                ", data=" + data +
                '}';
    }
}
