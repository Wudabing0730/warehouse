/**
 * P3-1 核心业务功能测试:JwtTokenProvider
 *
 * 覆盖范围:
 *   - generateAccessToken:生成的 token 必须能被 verify
 *   - validateAccessToken:合法 token → true;非法 token → false
 *   - getUserIdFromToken:round-trip 一致
 *   - getUsernameFromToken:round-trip 一致
 *   - getJtiFromToken:JWT ID 存在
 *   - getExpirationFromToken:在 1 分钟过期时间之内
 */
package com.warehouse.core;

import com.warehouse.config.JwtProperties;
import com.warehouse.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        // HMAC512 至少需要 64 字节的 secret
        props.setAccessSecret("test-secret-must-be-at-least-64-bytes-long-for-HMAC512-algorithm-padding");
        props.setAccessExpiration(60_000L); // 1 分钟
        provider = new JwtTokenProvider(props);
    }

    @Test
    @DisplayName("generateAccessToken 生成的 token 必须能被验证通过")
    void generatedToken_mustBeValid() {
        String token = provider.generateAccessToken(1L, "admin");
        assertNotNull(token);
        assertTrue(provider.validateAccessToken(token));
    }

    @Test
    @DisplayName("getUserIdFromToken 必须 round-trip 一致")
    void getUserIdFromToken_roundTrip() {
        String token = provider.generateAccessToken(42L, "testuser");
        assertEquals(42L, provider.getUserIdFromToken(token).longValue());
    }

    @Test
    @DisplayName("getUsernameFromToken 必须 round-trip 一致")
    void getUsernameFromToken_roundTrip() {
        String token = provider.generateAccessToken(1L, "alice");
        assertEquals("alice", provider.getUsernameFromToken(token));
    }

    @Test
    @DisplayName("getJtiFromToken 必须返回非空 UUID 字符串")
    void getJtiFromToken_notEmpty() {
        String token = provider.generateAccessToken(1L, "admin");
        String jti = provider.getJtiFromToken(token);
        assertNotNull(jti);
        assertFalse(jti.isEmpty());
    }

    @Test
    @DisplayName("getExpirationFromToken 必须在 1 分钟过期窗口内")
    void getExpirationFromToken_withinWindow() {
        String token = provider.generateAccessToken(1L, "admin");
        Date exp = provider.getExpirationFromToken(token);
        assertNotNull(exp);
        long now = System.currentTimeMillis();
        // expiration 应在 (now, now+60s) 窗口内
        assertTrue(exp.getTime() > now, "expiration 必须在 now 之后");
        assertTrue(exp.getTime() - now <= 60_000L, "expiration 不能超过 1 分钟");
    }

    @Test
    @DisplayName("非法 token 必须 validate 失败")
    void invalidToken_mustFail() {
        assertFalse(provider.validateAccessToken("invalid.token.string"));
    }

    @Test
    @DisplayName("不同用户的 token 必须有不同 jti(UUID 唯一)")
    void differentUsers_getDifferentJti() {
        String t1 = provider.generateAccessToken(1L, "alice");
        String t2 = provider.generateAccessToken(2L, "bob");
        assertNotEquals(provider.getJtiFromToken(t1), provider.getJtiFromToken(t2));
    }
}
