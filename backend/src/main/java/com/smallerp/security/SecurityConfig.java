package com.smallerp.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smallerp.common.ErrorCode;
import com.smallerp.common.ErrorResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 백엔드 하나를 경로로 나눈다.
 * /api/admin/** 관리자 서비스, /api/app/** 사용자 서비스, /api/public/** 로그인 없이 쓰는 API.
 */
@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Order(1)
    SecurityFilterChain adminChain(HttpSecurity http, TokenService tokens, ObjectMapper om) throws Exception {
        return apiChain(http, "/api/admin/**", TokenAudience.ADMIN, tokens, om)
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/api/admin/auth/login", "/api/admin/auth/refresh", "/api/admin/auth/logout").permitAll()
                        .anyRequest().authenticated())
                .build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain appChain(HttpSecurity http, TokenService tokens, ObjectMapper om) throws Exception {
        return apiChain(http, "/api/app/**", TokenAudience.APP, tokens, om)
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/api/app/auth/login", "/api/app/auth/refresh", "/api/app/auth/logout").permitAll()
                        .anyRequest().authenticated())
                .build();
    }

    @Bean
    @Order(3)
    SecurityFilterChain publicChain(HttpSecurity http) throws Exception {
        return http
                .csrf(c -> c.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/api/public/**", "/actuator/health", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().denyAll())
                .build();
    }

    /*
     * 토큰은 httpOnly + SameSite=Lax 쿠키로 주고받는다. 다른 사이트에서 보낸 POST에는 쿠키가 실리지 않아
     * CSRF 토큰 없이 시작한다. 프론트는 같은 도메인에서 /api 를 백엔드로 프록시한다.
     */
    private HttpSecurity apiChain(HttpSecurity http, String pattern, TokenAudience audience,
                                  TokenService tokens, ObjectMapper om) throws Exception {
        AuthenticationEntryPoint unauthorized = (req, res, ex) -> {
            res.setStatus(ErrorCode.UNAUTHORIZED.status().value());
            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
            res.setCharacterEncoding("UTF-8");
            om.writeValue(res.getWriter(), ErrorResponse.of(ErrorCode.UNAUTHORIZED));
        };
        return http
                .securityMatcher(pattern)
                .csrf(c -> c.disable())
                .httpBasic(b -> b.disable())
                .formLogin(f -> f.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e.authenticationEntryPoint(unauthorized))
                .oauth2ResourceServer(o -> o
                        .bearerTokenResolver(new CookieOrHeaderTokenResolver(audience))
                        .authenticationEntryPoint(unauthorized)
                        .jwt(j -> j.decoder(tokens.accessDecoderFor(audience))));
    }
}
