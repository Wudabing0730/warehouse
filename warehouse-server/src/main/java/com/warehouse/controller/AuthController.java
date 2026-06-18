package com.warehouse.controller;

import com.warehouse.common.Result;
import com.warehouse.dto.request.LoginRequest;
import com.warehouse.dto.request.RefreshTokenRequest;
import com.warehouse.dto.response.LoginResponse;
import com.warehouse.dto.response.TokenResponse;
import com.warehouse.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication management APIs")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate with username and password to obtain access and refresh tokens")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return Result.success(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Exchange a valid refresh token for a new access token pair")
    public Result<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        TokenResponse response = authService.refreshToken(request);
        return Result.success(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Invalidate the current access token")
    public Result<Void> logout(@RequestHeader("Authorization") String authHeader) {
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        authService.logout(token);
        return Result.success();
    }
}
