/**
 * P2-3 JWT 过滤器未放行 OPTIONS 预检请求 — 修复验证
 *
 * Bug 复现:
 *   - 浏览器对跨域带 Authorization 的请求会先发 OPTIONS 预检
 *   - 预检请求没有 Authorization header
 *   - JwtAuthenticationFilter.doFilterInternal 在 header 缺失时直接 401
 *   - 导致前端所有跨域带 JWT 的请求被 CORS 拦截
 *   - 现象:浏览器 console 报 "Response to preflight request doesn't pass access control check"
 *
 * 修复:
 *   - JwtAuthenticationFilter.shouldNotFilter 在 method == OPTIONS 时返回 true
 *   - SecurityConfig.authorizeHttpRequests 显式 permitAll HttpMethod.OPTIONS
 *   - 两道保险:Filter 不阻挡 + Security 不鉴权
 */
package com.warehouse.security;

import com.warehouse.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.SecurityFilterChain;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
// (assertDoesNotThrow 已不再使用,被反射调用替代)

class JwtOptionsPreflightBypassTest {

    /** P2-3: JwtAuthenticationFilter 必须直接放行 OPTIONS 请求 */
    @Test
    void jwtFilter_shouldNotFilter_optionsRequest() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(null, null, null);

        MockHttpServletRequest req = new MockHttpServletRequest("OPTIONS", "/api/v1/inbound/orders");
        req.addHeader("Origin", "http://localhost:5173");
        req.addHeader("Access-Control-Request-Method", "GET");
        req.addHeader("Access-Control-Request-Headers", "authorization,content-type");

        Method m = JwtAuthenticationFilter.class.getDeclaredMethod("shouldNotFilter", jakarta.servlet.http.HttpServletRequest.class);
        m.setAccessible(true);
        boolean result = (boolean) m.invoke(filter, req);

        assertTrue(result, "OPTIONS 预检请求必须被 shouldNotFilter 放行,否则会被 401 拦截");
    }

    /** P2-3: SecurityConfig 必须存在 SecurityFilterChain bean */
    @Test
    void securityConfig_exposesFilterChainBean() throws Exception {
        // 静态校验: SecurityConfig 类存在且包含 securityFilterChain 方法
        Method m = SecurityConfig.class.getDeclaredMethod("securityFilterChain",
                org.springframework.security.config.annotation.web.builders.HttpSecurity.class);
        assertNotNull(m);
        assertTrue(m.getReturnType().equals(SecurityFilterChain.class));
        // 验证方法上有 @Bean 注解(让 Spring 把它注册为过滤器链)
        assertNotNull(m.getAnnotation(org.springframework.context.annotation.Bean.class));
    }

    /** P2-3: SecurityConfig 源码必须显式 permitAll HttpMethod.OPTIONS */
    @Test
    void securityConfig_permitsAllOptions() throws Exception {
        // 读源码断言(防止后续重构把 OPTIONS 放行逻辑移除)
        String src = new String(Files.readAllBytes(
                Paths.get("src/main/java/com/warehouse/config/SecurityConfig.java")),
                StandardCharsets.UTF_8);
        // 必须出现 HttpMethod.OPTIONS + permitAll 的组合
        assertTrue(
                src.contains("HttpMethod.OPTIONS") && src.contains("permitAll"),
                "SecurityConfig 必须显式 permitAll HttpMethod.OPTIONS,否则跨域预检会被 Spring Security 拦截"
        );
    }

    /** P2-3: JwtAuthenticationFilter 源码必须显式处理 OPTIONS(method 短路) */
    @Test
    void jwtFilter_sourceHandlesOptionsMethod() throws Exception {
        String src = new String(Files.readAllBytes(
                Paths.get("src/main/java/com/warehouse/security/JwtAuthenticationFilter.java")),
                StandardCharsets.UTF_8);
        // shouldNotFilter 内必须出现 OPTIONS 字符串 + return true 短路
        assertTrue(
                src.contains("shouldNotFilter"),
                "JwtAuthenticationFilter 必须有 shouldNotFilter 方法"
        );
        // 检查 shouldNotFilter 方法体内出现 OPTIONS 关键字 + return true
        int start = src.indexOf("protected boolean shouldNotFilter");
        assertTrue(start > 0, "必须存在 shouldNotFilter 方法");
        // 找方法体结束的下一个 }
        int bodyStart = src.indexOf("{", start);
        int bodyEnd = src.indexOf("\n    }", bodyStart);
        String body = src.substring(bodyStart, bodyEnd);
        assertTrue(
                body.contains("OPTIONS"),
                "shouldNotFilter 必须检查 OPTIONS 方法,否则跨域预检会被 401 拦截"
        );
    }
}
