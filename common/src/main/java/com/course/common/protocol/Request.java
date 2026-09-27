package com.course.common.protocol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.course.common.util.JsonUtil;

import java.io.Serializable;
import java.util.UUID;

/**
 * TCP Request message framing structure.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Request implements Serializable {
    private static final long serialVersionUID = 1L;

    private Action action;
    private String requestId;
    private JsonNode data;

    public Request() {
    }

    public Request(Action action, String requestId, JsonNode data) {
        this.action = action;
        this.requestId = requestId;
        this.data = data;
    }

    public static Request of(Action action, Object dataObj) {
        String reqId = "REQ-" + UUID.randomUUID().toString().substring(0, 8);
        JsonNode node = dataObj != null ? JsonUtil.getObjectMapper().valueToTree(dataObj) : null;
        return new Request(action, reqId, node);
    }

    public static Request of(Action action, String requestId, Object dataObj) {
        JsonNode node = dataObj != null ? JsonUtil.getObjectMapper().valueToTree(dataObj) : null;
        return new Request(action, requestId, node);
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
        return "Request{" +
                "action=" + action +
                ", requestId='" + requestId + '\'' +
                ", data=" + data +
                '}';
    }
}
