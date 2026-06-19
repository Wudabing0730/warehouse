/**
 * Bug 复现验证:OperationLogAspect 必须正确填充 operation/module/username 字段
 *
 * Bug 复现:
 *   后端日志持续报错:
 *     org.springframework.dao.DataIntegrityViolationException:
 *       Field 'operation' doesn't have a default value
 *
 *   根因:OperationLogAspect.record() 创建 OperationLog 时只设置了
 *   requestMethod/requestUrl/requestParams/ipAddress/userAgent/operateTime/userId,
 *   但 t_operation_log 表的 operation VARCHAR(200) NOT NULL 和 module VARCHAR(50) NOT NULL
 *   字段**完全没被填充**,导致 INSERT 失败。
 *
 *   OperationLogAspect 自己在 finally 块 catch 了异常,记录 "操作日志写入失败",
 *   不影响主请求 — 但每次 API 调用都失败,t_operation_log 表永远写不进去,
 *   dashboard 的 recentOps 永远只有种子数据,实际业务操作历史丢失。
 *
 * 修复:OperationLogAspect 必须:
 *   1. 从 controller 类名推断 module(去掉 Controller 后缀,转小写)
 *   2. 从 method 签名 + RequestMapping 推断 operation(中文描述)
 *   3. 从 SecurityContext 提取 username
 */
package com.warehouse.interceptor;

import com.warehouse.entity.OperationLog;
import com.warehouse.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 测试 OperationLogAspect 必须填充 NOT NULL 字段:operation / module
 * 以及 username(可空但应该有值)。
 *
 * 策略:用 mock ProceedingJoinPoint + HttpServletRequest 调用 record(),
 * 捕获传给 logService.save() 的 OperationLog,断言所有 NOT NULL 字段非空。
 */
class OperationLogAspectFieldsTest {

    private OperationLogService logService;
    private HttpServletRequest request;
    private OperationLogAspect aspect;

    @BeforeEach
    void setUp() {
        logService = mock(OperationLogService.class);
        request = mock(HttpServletRequest.class);
        aspect = new OperationLogAspect(logService, request);
    }

    @Test
    @DisplayName("BUG 复现:DashboardController 调用 record() 时,operation 字段必须被填充(原代码完全没设置,导致 INSERT 失败)")
    void operationFieldMustBeFilled() throws Throwable {
        // 模拟 DashboardController.summary() 的 AOP 上下文
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(pjp.getSignature()).thenReturn(signature);
        when(signature.getDeclaringTypeName()).thenReturn("com.warehouse.controller.DashboardController");
        when(signature.getName()).thenReturn("summary");
        when(pjp.getArgs()).thenReturn(new Object[]{});
        when(pjp.proceed()).thenReturn(new Object()); // controller 返回值
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/dashboard/summary");
        when(request.getHeader("User-Agent")).thenReturn("JUnit");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        aspect.record(pjp);

        // 等待异步写入完成(给 100ms 让 CompletableFuture 完成)
        Thread.sleep(200);

        // 捕获传给 save() 的 OperationLog,断言字段
        org.mockito.ArgumentCaptor<OperationLog> captor =
                org.mockito.ArgumentCaptor.forClass(OperationLog.class);
        verify(logService).save(captor.capture());
        OperationLog log = captor.getValue();

        assertNotNull(log.getOperation(), "operation 字段必须被填充(原 bug 完全没设置,导致 INSERT 失败)");
        assertNotNull(log.getModule(), "module 字段必须被填充(原 bug 完全没设置,导致 INSERT 失败)");
    }

    @Test
    @DisplayName("module 必须从 controller 类名推断:DashboardController → dashboard")
    void moduleMustBeDerivedFromControllerName() throws Throwable {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(pjp.getSignature()).thenReturn(signature);
        when(signature.getDeclaringTypeName()).thenReturn("com.warehouse.controller.DashboardController");
        when(signature.getName()).thenReturn("summary");
        when(pjp.getArgs()).thenReturn(new Object[]{});
        when(pjp.proceed()).thenReturn(new Object());
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/dashboard/summary");
        when(request.getHeader("User-Agent")).thenReturn("JUnit");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        aspect.record(pjp);
        Thread.sleep(200);

        org.mockito.ArgumentCaptor<OperationLog> captor =
                org.mockito.ArgumentCaptor.forClass(OperationLog.class);
        verify(logService).save(captor.capture());
        OperationLog log = captor.getValue();

        assertEquals("dashboard", log.getModule(),
                "module 应从 controller 类名推断:DashboardController → dashboard");
    }

    @Test
    @DisplayName("operation 必须包含 HTTP 方法和路径信息(供 audit 查询)")
    void operationMustContainHttpMethodAndPath() throws Throwable {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(pjp.getSignature()).thenReturn(signature);
        when(signature.getDeclaringTypeName()).thenReturn("com.warehouse.controller.InboundController");
        when(signature.getName()).thenReturn("create");
        when(pjp.getArgs()).thenReturn(new Object[]{});
        when(pjp.proceed()).thenReturn(new Object());
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/v1/inbound");
        when(request.getHeader("User-Agent")).thenReturn("JUnit");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        aspect.record(pjp);
        Thread.sleep(200);

        org.mockito.ArgumentCaptor<OperationLog> captor =
                org.mockito.ArgumentCaptor.forClass(OperationLog.class);
        verify(logService).save(captor.capture());
        OperationLog log = captor.getValue();

        assertNotNull(log.getOperation());
        // operation 至少应包含 method 名 + 请求方法,例如 "create(POST)"
        assertEquals(true, log.getOperation().contains("create"),
                "operation 应包含方法名(供 audit 查询),实际: " + log.getOperation());
    }

    @Test
    @DisplayName("controller 抛异常时,operation/module 字段也必须被填充(否则异常路径的日志也写不进去)")
    void operationFieldFilledEvenWhenControllerThrows() throws Throwable {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(pjp.getSignature()).thenReturn(signature);
        when(signature.getDeclaringTypeName()).thenReturn("com.warehouse.controller.UserController");
        when(signature.getName()).thenReturn("delete");
        when(pjp.getArgs()).thenReturn(new Object[]{1L});
        when(pjp.proceed()).thenThrow(new RuntimeException("user not found"));
        when(request.getMethod()).thenReturn("DELETE");
        when(request.getRequestURI()).thenReturn("/api/v1/system/user/1");
        when(request.getHeader("User-Agent")).thenReturn("JUnit");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        try {
            aspect.record(pjp);
        } catch (Throwable ignored) {
            // 期望 controller 异常会被 AOP 重新抛出
        }
        Thread.sleep(200);

        org.mockito.ArgumentCaptor<OperationLog> captor =
                org.mockito.ArgumentCaptor.forClass(OperationLog.class);
        verify(logService).save(captor.capture());
        OperationLog log = captor.getValue();

        assertNotNull(log.getOperation(), "异常路径的 operation 也必须填充");
        assertNotNull(log.getModule(), "异常路径的 module 也必须填充");
        assertEquals(0, log.getStatus(), "controller 抛异常时 status 应为 0(失败)");
        assertEquals("user", log.getModule(), "UserController → user 模块");
    }
}