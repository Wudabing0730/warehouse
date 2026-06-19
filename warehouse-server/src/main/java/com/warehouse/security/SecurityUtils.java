package com.warehouse.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User is not authenticated");
        }
        // 修复:JwtAuthenticationFilter 把 UserDetails 对象塞进 principal,
        // 用 principal.toString() 会得到 "org.springframework.security.core.userdetails.User [Username=1, ...]"
        // 这种字符串,Long.valueOf() 会抛 NumberFormatException。
        // 应改用 authentication.getName() —— 在 username="1" 的情况下直接返回 "1"。
        return Long.valueOf(authentication.getName());
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
