package com.warehouse.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.warehouse.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    public String generateAccessToken(Long userId, String username) {
        Algorithm algorithm = Algorithm.HMAC512(jwtProperties.getAccessSecret());
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtProperties.getAccessExpiration());

        return JWT.create()
                .withSubject(String.valueOf(userId))
                .withClaim("username", username)
                .withJWTId(UUID.randomUUID().toString())
                .withIssuedAt(now)
                .withExpiresAt(expiration)
                .sign(algorithm);
    }

    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    public boolean validateAccessToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC512(jwtProperties.getAccessSecret());
            JWTVerifier verifier = JWT.require(algorithm).build();
            verifier.verify(token);
            return true;
        } catch (JWTVerificationException e) {
            return false;
        }
    }

    public Long getUserIdFromToken(String token) {
        Algorithm algorithm = Algorithm.HMAC512(jwtProperties.getAccessSecret());
        JWTVerifier verifier = JWT.require(algorithm).build();
        DecodedJWT jwt = verifier.verify(token);
        return Long.valueOf(jwt.getSubject());
    }

    public String getUsernameFromToken(String token) {
        Algorithm algorithm = Algorithm.HMAC512(jwtProperties.getAccessSecret());
        JWTVerifier verifier = JWT.require(algorithm).build();
        DecodedJWT jwt = verifier.verify(token);
        return jwt.getClaim("username").asString();
    }

    public String getJtiFromToken(String token) {
        Algorithm algorithm = Algorithm.HMAC512(jwtProperties.getAccessSecret());
        JWTVerifier verifier = JWT.require(algorithm).build();
        DecodedJWT jwt = verifier.verify(token);
        return jwt.getId();
    }

    public Date getExpirationFromToken(String token) {
        Algorithm algorithm = Algorithm.HMAC512(jwtProperties.getAccessSecret());
        JWTVerifier verifier = JWT.require(algorithm).build();
        DecodedJWT jwt = verifier.verify(token);
        return jwt.getExpiresAt();
    }
}
