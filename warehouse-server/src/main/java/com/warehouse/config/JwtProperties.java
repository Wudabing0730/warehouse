package com.warehouse.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String accessSecret;

    private String refreshSecret;

    private Long accessExpiration = 900000L;

    private Long refreshExpiration = 604800000L;
}
