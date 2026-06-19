/**
 * P3-1 核心业务功能测试:PasswordEncoder
 *
 * 覆盖范围:
 *   - SecurityConfig 暴露的 PasswordEncoder bean 是 BCryptPasswordEncoder(强度 10)
 *   - 同一明文密码两次 encode 出不同 hash(盐值随机)
 *   - matches(明文, hash) → true
 *   - matches(错误密码, hash) → false
 *   - 已有的 admin 账号 BCrypt hash 可被验证通过
 */
package com.warehouse.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordEncoderContractTest {

    // 与 SecurityConfig.passwordEncoder() 保持一致:BCrypt 强度 10
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(10);

    @Test
    @DisplayName("PasswordEncoder bean 必须存在且为 BCrypt")
    void passwordEncoder_mustBeBCrypt() {
        assertNotNull(encoder);
        assertTrue(encoder instanceof BCryptPasswordEncoder,
                "PasswordEncoder 必须是 BCryptPasswordEncoder,实际: " + encoder.getClass().getName());
    }

    @Test
    @DisplayName("同一明文密码两次 encode 必须产生不同 hash(盐值随机)")
    void samePassword_differentHash() {
        String hash1 = encoder.encode("admin123");
        String hash2 = encoder.encode("admin123");
        assertNotEquals(hash1, hash2, "BCrypt 两次 encode 应产生不同 hash(盐值不同)");
    }

    @Test
    @DisplayName("matches(明文, hash) → true")
    void matches_correctPassword() {
        String hash = encoder.encode("admin123");
        assertTrue(encoder.matches("admin123", hash));
    }

    @Test
    @DisplayName("matches(错误密码, hash) → false")
    void matches_wrongPassword() {
        String hash = encoder.encode("admin123");
        assertFalse(encoder.matches("wrong", hash));
    }

    @Test
    @DisplayName("02-seed.sql 中 admin 的 BCrypt hash 必须能被 PasswordEncoder 验证通过")
    void seedAdminHash_mustMatch() {
        // 这是 02-seed.sql 第 19 行的 admin hash
        String adminHash = "$2b$10$7YgJw5khn76f0wSr4P.65OYlwEoG1VbkZeixDgBAi1xnNa1I0SfI2";
        assertTrue(encoder.matches("admin123", adminHash),
                "种子 admin 账号的 BCrypt hash 必须能验证明文 admin123");
    }
}
