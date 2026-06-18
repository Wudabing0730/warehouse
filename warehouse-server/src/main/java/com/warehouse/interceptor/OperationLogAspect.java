package com.warehouse.interceptor;

import cn.hutool.json.JSONUtil;
import com.warehouse.entity.OperationLog;
import com.warehouse.security.SecurityUtils;
import com.warehouse.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogService logService;
    private final HttpServletRequest request;

    @Around("execution(* com.warehouse.controller.*.*(..))")
    public Object record(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        String requestParams = truncate(JSONUtil.toJsonStr(pjp.getArgs()), 2000);

        OperationLog operationLog = new OperationLog();
        operationLog.setRequestMethod(request.getMethod());
        operationLog.setRequestUrl(request.getRequestURI());
        operationLog.setRequestParams(requestParams);
        operationLog.setIpAddress(getClientIp(request));
        operationLog.setUserAgent(request.getHeader("User-Agent"));
        operationLog.setOperateTime(LocalDateTime.now());

        try {
            Long userId = SecurityUtils.getCurrentUserIdSafely();
            operationLog.setUserId(userId != null ? userId : 0L);
        } catch (Exception e) {
            operationLog.setUserId(0L);
        }

        Object result;
        try {
            result = pjp.proceed();
            operationLog.setExecutionTime((int) (System.currentTimeMillis() - start));
            operationLog.setStatus(1);
            operationLog.setResponseResult(truncate(JSONUtil.toJsonStr(result), 2000));
        } catch (Exception e) {
            operationLog.setExecutionTime((int) (System.currentTimeMillis() - start));
            operationLog.setStatus(0);
            operationLog.setResponseResult(truncate(e.getMessage(), 500));
            throw e;
        } finally {
            // 异步写入，避免阻塞业务
            CompletableFuture.runAsync(() -> {
                try {
                    logService.save(operationLog);
                } catch (Exception e) {
                    log.error("操作日志写入失败", e);
                }
            });
        }
        return result;
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return null;
        return str.length() > maxLen ? str.substring(0, maxLen) + "..." : str;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
