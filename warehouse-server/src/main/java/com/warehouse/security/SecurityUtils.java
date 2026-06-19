package com.warehouse.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User is not authenticated");
        }
        return Long.valueOf(authentication.getPrincipal().toString());
    }

    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User is not authenticated");
        }
        return authentication.getName();
    }

    public static Long getCurrentUserIdSafely() {
        try {
            return getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }

    public static String getCurrentUsernameSafely() {
        try {
            return getCurrentUsername();
        } catch (Exception e) {
            return null;
        }
    }
}
