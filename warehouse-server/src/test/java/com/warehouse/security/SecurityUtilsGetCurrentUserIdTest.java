/**
 * Bug 复现验证:SecurityUtils.getCurrentUserId() 在 principal 是 UserDetails 对象时抛 NumberFormatException
 *
 * 用户报告:"禁用 P001 等所有数据更改操作都无法正常持久化"
 *
 * 根因(已通过后端日志定位):
 *   - 后端日志(backend.log)出现:
 *       java.lang.NumberFormatException: For input string:
 *         "org.springframework.security.core.userdetails.User [Username=1, Password=[PROTECTED], ...]"
 *   - 触发链:
 *     1. JwtAuthenticationFilter 验证 token 后,从 UserDetailsServiceImpl.loadUserByUsername 加载用户
 *     2. 加载到的是 org.springframework.security.core.userdetails.User(标准 Spring Security 类)
 *     3. JwtAuthenticationFilter 把这个 User 对象塞进 AuthenticationToken 作为 principal
 *     4. ProductController.update/delete 上有 @RequirePermission 注解
 *     5. PermissionAspect 调 SecurityUtils.getCurrentUserId() 获取当前用户 ID
 *     6. SecurityUtils.getCurrentUserId() 用 Long.valueOf(principal.toString())
 *        但 principal.toString() 是 "org.springframework.security.core.userdetails.User [Username=1, ...]"
 *     7. Long.valueOf() 抛 NumberFormatException
 *     8. PermissionAspect 没 catch,异常冒泡到 GlobalExceptionHandler 兜底为 500
 *     9. 同时 @Transactional 触发回滚,service.update() 里的所有 DB 写入被回滚
 *     10. 前端看到 500 + 通用"服务器内部错误",数据"看起来没变"
 *
 *   - 为什么 GET 成功?GET 路径无 @RequirePermission,PermissionAspect 不执行
 *   - 为什么 application.yml 修复后仍报 500?那是不同的 bug
 *
 * 修复:SecurityUtils.getCurrentUserId() 应该用 authentication.getName()(返回 username "1"),
 *      而不是 principal.toString()(返回 User 对象的字符串表示)
 *
 * 验证策略:模拟 Authentication 的 principal 是 UserDetails 对象,断言 getCurrentUserId 不抛异常
 */
package com.warehouse.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SecurityUtilsGetCurrentUserIdTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * BUG 复现:principal 是 Spring Security 的 UserDetails 对象时,
     * getCurrentUserId() 用 Long.valueOf(principal.toString()) 抛 NumberFormatException
     *
     * 这是 JwtAuthenticationFilter 的真实情况:把 UserDetails 塞进 AuthenticationToken
     */
    @Test
    @DisplayName("BUG 复现:principal 是 UserDetails 对象时,getCurrentUserId() 必须能正确解析为 Long,不抛 NumberFormatException")
    void getCurrentUserIdMustParseUserDetailsPrincipal() {
        // 模拟 JwtAuthenticationFilter 的实际行为:
        // 1) 加载 userDetails
        User userDetails = new User("1", "password", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        // 2) 塞进 AuthenticationToken 作为 principal
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // 关键:不抛 NumberFormatException
        Long userId = assertDoesNotThrow(SecurityUtils::getCurrentUserId,
                "BUG 复现:principal 是 UserDetails 时,getCurrentUserId() 不应抛 NumberFormatException。"
                        + " 当前用 Long.valueOf(principal.toString()) 解析 Spring Security User 对象,"
                        + " 会得到 'org.springframework.security.core.userdetails.User [Username=1, ...]'"
                        + " 这种字符串,无法转 Long。修复方法:用 authentication.getName() 替代。");
        assertNotNull(userId);
        assertEquals(1L, userId, "username='1' 应解析为 userId=1");
    }

    @Test
    @DisplayName("getCurrentUserIdSafely 在 principal 是 UserDetails 时也必须返回正确的 userId,不抛异常")
    void getCurrentUserIdSafelyMustParseUserDetailsPrincipal() {
        User userDetails = new User("42", "password", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        Long userId = assertDoesNotThrow(SecurityUtils::getCurrentUserIdSafely,
                "BUG:getCurrentUserIdSafely() 同样依赖 principal.toString() 解析,会抛 NumberFormatException");
        assertNotNull(userId);
        assertEquals(42L, userId);
    }

    @Test
    @DisplayName("principal 是纯数字字符串时(兼容性测试),getCurrentUserId() 仍能正确解析")
    void getCurrentUserIdMustParseStringPrincipal() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "1", null, List.<GrantedAuthority>of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        Long userId = SecurityUtils.getCurrentUserId();
        assertEquals(1L, userId);
    }
}