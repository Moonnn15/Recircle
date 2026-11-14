package com.recircle.security;

import com.recircle.enums.UserRole;

public class AuthPrincipal {
    private final Long userId;
    private final String email;
    private final UserRole role;
    private final boolean campusVerified;

    public AuthPrincipal(Long userId, String email, UserRole role, boolean campusVerified) {
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.campusVerified = campusVerified;
    }

    public Long getUserId() { return userId; }
    public String getEmail() { return email; }
    public UserRole getRole() { return role; }
    public boolean isCampusVerified() { return campusVerified; }
}
