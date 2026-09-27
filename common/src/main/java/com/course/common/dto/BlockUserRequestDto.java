package com.course.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BlockUserRequestDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String identifier; // StudentId, Username or Email
    private boolean blocked;   // true = block, false = unblock

    public BlockUserRequestDto() {
    }

    public BlockUserRequestDto(String identifier, boolean blocked) {
        this.identifier = identifier;
        this.blocked = blocked;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }
}
