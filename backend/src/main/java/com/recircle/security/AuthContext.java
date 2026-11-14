package com.recircle.security;

import com.recircle.enums.UserRole;
import com.recircle.exception.ApiException;
import org.springframework.http.HttpStatus;

public class AuthContext {

    private static final ThreadLocal<AuthPrincipal> CURRENT = new ThreadLocal<>();

    public static void set(AuthPrincipal principal) {
        CURRENT.set(principal);
    }

    public static AuthPrincipal get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }

    public static AuthPrincipal requireAuth() {
        AuthPrincipal p = CURRENT.get();
        if (p == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication token is missing or invalid.");
        }
        return p;
    }

    public static AuthPrincipal requireRole(UserRole... allowedRoles) {
        AuthPrincipal p = requireAuth();
        for (UserRole r : allowedRoles) {
            if (p.getRole() == r) {
                return p;
            }
        }
        throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have permission to perform this operation.");
    }
}
