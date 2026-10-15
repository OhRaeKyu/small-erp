package com.smallerp.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Cookie cookie, Seed seed) {

    public record Jwt(String secret, Duration accessTokenTtl, Duration refreshTokenTtl) {
    }

    public record Cookie(boolean secure) {
    }

    public record Seed(boolean enabled, String adminEmail, String adminPassword) {
    }
}
