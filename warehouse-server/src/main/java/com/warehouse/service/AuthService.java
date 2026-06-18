package com.warehouse.service;

import com.warehouse.dto.request.LoginRequest;
import com.warehouse.dto.request.RefreshTokenRequest;
import com.warehouse.dto.response.LoginResponse;
import com.warehouse.dto.response.TokenResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    TokenResponse refreshToken(RefreshTokenRequest request);

    void logout(String accessToken);
}
