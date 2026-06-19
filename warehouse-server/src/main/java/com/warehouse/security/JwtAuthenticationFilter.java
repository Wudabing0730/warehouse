package com.warehouse.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsServiceImpl userDetailsService;
    private final StringRedisTemplate stringRedisTemplate;

    private static final List<String> SKIP_PATHS = Arrays.asList(
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/v3/api-docs/**",
            "/swagger-ui/**"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // P2-3: CORS 预检 OPTIONS 请求没有 Authorization header,如果走完整 doFilterInternal
        // 会被直接 401 拦截,导致前端所有跨域带 JWT 的请求失败。
        // 预检请求由 CorsConfig / Security 层的 permitAll 处理,JWT 过滤器必须直接放行。
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getServletPath();
        return SKIP_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendUnauthorized(response, "Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(7);

        if (!jwtTokenProvider.validateAccessToken(token)) {
            sendUnauthorized(response, "Invalid or expired token");
            return;
        }

        String jti = jwtTokenProvider.getJtiFromToken(token);
        String blacklistKey = "blacklist:access:" + jti;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(blacklistKey))) {
            sendUnauthorized(response, "Token has been revoked");
            return;
        }

        String username = jwtTokenProvider.getUsernameFromToken(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> body = new HashMap<>();
        body.put("code", 401);
        body.put("message", message);

        ObjectMapper objectMapper = new ObjectMapper();
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
