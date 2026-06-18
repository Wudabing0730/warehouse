package com.warehouse.interceptor;

import com.warehouse.annotation.RequirePermission;
import com.warehouse.exception.BusinessException;
import com.warehouse.security.SecurityUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PermissionAspect {

    private static final Logger log = LoggerFactory.getLogger(PermissionAspect.class);

    @Around("@annotation(requirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint, RequirePermission requirePermission) throws Throwable {
        String permissionCode = requirePermission.value();
        Long userId = SecurityUtils.getCurrentUserId();

        // TODO: Inject userService to check actual permissions
        // For now, log a warning and allow the request
        log.warn("Permission check not yet implemented for userId={}, permission={}. Allowing request.",
                userId, permissionCode);

        return joinPoint.proceed();
    }
}
