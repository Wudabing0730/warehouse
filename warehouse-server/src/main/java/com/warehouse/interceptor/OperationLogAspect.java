package com.warehouse.interceptor;

import cn.hutool.json.JSONUtil;
import com.warehouse.entity.OperationLog;
import com.warehouse.security.SecurityUtils;
import com.warehouse.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
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

        // 修复:Bug — operation/module 字段必须被填充,否则 t_operation_log INSERT 失败
        // (operation VARCHAR(200) NOT NULL, module VARCHAR(50) NOT NULL)
        Signature signature = pjp.getSignature();
        String module = deriveModule(signature);
        String operation = deriveOperation(signature, request);

        OperationLog operationLog = new OperationLog();
        operationLog.setModule(module);
        operationLog.setOperation(operation);
        operationLog.setRequestMethod(request.getMethod());
        operationLog.setRequestUrl(request.getRequestURI());
        operationLog.setRequestParams(requestParams);
        operationLog.setIpAddress(getClientIp(request));
        operationLog.setUserAgent(request.getHeader("User-Agent"));
        operationLog.setOperateTime(LocalDateTime.now());

        try {
            Long userId = SecurityUtils.getCurrentUserIdSafely();
            operationLog.setUserId(userId != null ? userId : 0L);
            String username = SecurityUtils.getCurrentUsernameSafely();
            operationLog.setUsername(username != null ? username : "anonymous");
        } catch (Exception e) {
            operationLog.setUserId(0L);
            operationLog.setUsername("anonymous");
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
            // 异步写入,避免阻塞业务
            CompletableFuture.runAsync(() -> {
                try {
                    logService.save(operationLog);
                } catch (Exception ex) {
                    log.error("操作日志写入失败", ex);
                }
            });
        }
        return result;
    }

    /**
     * 从 controller 类名推断 module
     * 规则:取类名最后一段,去掉 Controller 后缀,转小写
     * 例:com.warehouse.controller.DashboardController → dashboard
     * 例:com.warehouse.controller.InboundController → inbound
     */
    private String deriveModule(Signature signature) {
        String fullClassName = signature.getDeclaringTypeName();
        String simpleClassName = fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
        String module = simpleClassName;
        if (module.endsWith("Controller")) {
            module = module.substring(0, module.length() - "Controller".length());
        }
        return module.toLowerCase();
    }

    /**
     * 从 method 名 + HTTP method 推断 operation
     * 格式:methodName(HTTP_METHOD),例:summary(GET)、create(POST)
     */
    private String deriveOperation(Signature signature, HttpServletRequest request) {
        String methodName = signature.getName();
        String httpMethod = request.getMethod();
        return methodName + "(" + httpMethod + ")";
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
            ip = request.getRemoteAddr();
        }
        // 多级代理取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}