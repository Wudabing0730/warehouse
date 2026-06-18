package com.warehouse.interceptor;

import com.warehouse.annotation.RequirePermission;
import com.warehouse.common.BusinessException;
import com.warehouse.security.SecurityUtils;
import com.warehouse.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {

    private final UserService userService;

    @Around("@annotation(requirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint, RequirePermission requirePermission) throws Throwable {
        String permissionCode = requirePermission.value();
        Long userId = SecurityUtils.getCurrentUserId();

        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }

        Set<String> permissions = userService.getPermissionCodes(userId);
        if (!permissions.contains(permissionCode)) {
            throw new BusinessException(403, "权限不足: " + permissionCode);
        }

        return joinPoint.proceed();
    }
}
