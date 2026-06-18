package com.warehouse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.warehouse.common.BusinessException;
import com.warehouse.common.ResultCode;
import com.warehouse.config.JwtProperties;
import com.warehouse.dto.request.LoginRequest;
import com.warehouse.dto.request.RefreshTokenRequest;
import com.warehouse.dto.response.LoginResponse;
import com.warehouse.dto.response.TokenResponse;
import com.warehouse.entity.Permission;
import com.warehouse.entity.Role;
import com.warehouse.entity.User;
import com.warehouse.mapper.PermissionMapper;
import com.warehouse.mapper.RoleMapper;
import com.warehouse.mapper.UserMapper;
import com.warehouse.mapper.UserRoleMapper;
import com.warehouse.mapper.RefreshTokenMapper;
import com.warehouse.security.JwtTokenProvider;
import com.warehouse.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, request.getUsername())
        );

        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "用户名或密码错误");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "用户名或密码错误");
        }

        if (user.getStatus() != null && user.getStatus() != 1) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "账号已被禁用");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getUserId(), user.getUsername());
        String refreshToken = jwtTokenProvider.generateRefreshToken();

        String tokenHash = sha256(refreshToken);
        stringRedisTemplate.opsForValue().set(
                "refresh_token:" + tokenHash,
                String.valueOf(user.getUserId()),
                Duration.ofMillis(jwtProperties.getRefreshExpiration())
        );

        List<String> roles = getRolesByUserId(user.getUserId());
        List<String> permissions = getUserPermissions(user.getUserId());

        LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .roles(roles)
                .permissions(permissions)
                .build();

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessExpiration())
                .userInfo(userInfo)
                .build();
    }

    @Override
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        String tokenHash = sha256(request.getRefreshToken());

        String userIdStr = stringRedisTemplate.opsForValue().get("refresh_token:" + tokenHash);
        if (userIdStr == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "刷新令牌无效或已过期");
        }

        stringRedisTemplate.delete("refresh_token:" + tokenHash);

        Long userId = Long.valueOf(userIdStr);
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "用户不存在");
        }

        if (user.getStatus() != null && user.getStatus() != 1) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "账号已被禁用");
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getUserId(), user.getUsername());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken();

        String newTokenHash = sha256(newRefreshToken);
        stringRedisTemplate.opsForValue().set(
                "refresh_token:" + newTokenHash,
                String.valueOf(user.getUserId()),
                Duration.ofMillis(jwtProperties.getRefreshExpiration())
        );

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessExpiration())
                .build();
    }

    @Override
    public void logout(String accessToken) {
        if (accessToken != null && !accessToken.isEmpty()) {
            try {
                String jti = jwtTokenProvider.getJtiFromToken(accessToken);
                Date expiration = jwtTokenProvider.getExpirationFromToken(accessToken);
                long remainingMillis = expiration.getTime() - System.currentTimeMillis();

                if (remainingMillis > 0) {
                    stringRedisTemplate.opsForValue().set(
                            "blacklist:access:" + jti,
                            "revoked",
                            remainingMillis,
                            TimeUnit.MILLISECONDS
                    );
                }
            } catch (Exception e) {
                log.warn("Failed to blacklist access token: {}", e.getMessage());
            }
        }
    }

    private List<String> getRolesByUserId(Long userId) {
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return roleMapper.selectBatchIds(roleIds).stream()
                .map(Role::getRoleName)
                .collect(Collectors.toList());
    }

    private List<String> getUserPermissions(Long userId) {
        List<Permission> permissions = permissionMapper.selectByUserId(userId);
        return permissions.stream()
                .map(Permission::getPermissionCode)
                .collect(Collectors.toList());
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
