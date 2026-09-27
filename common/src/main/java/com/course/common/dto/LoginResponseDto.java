package com.course.common.dto;

import com.course.common.model.Role;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginResponseDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String studentId;
    private String username;
    private String fullName;
    private String email;
    private String className;
    private Role role;

    public LoginResponseDto() {
    }

    public LoginResponseDto(String studentId, String username, String fullName, String email, String className, Role role) {
        this.studentId = studentId;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.className = className;
        this.role = role;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
