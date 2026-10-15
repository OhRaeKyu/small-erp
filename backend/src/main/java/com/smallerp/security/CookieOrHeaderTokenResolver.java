package com.smallerp.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;

/** Authorization: Bearer 헤더를 먼저 보고, 없으면 서비스별 httpOnly 쿠키에서 토큰을 읽는다. */
class CookieOrHeaderTokenResolver implements BearerTokenResolver {

    private final DefaultBearerTokenResolver headerResolver = new DefaultBearerTokenResolver();
    private final String cookieName;

    CookieOrHeaderTokenResolver(TokenAudience audience) {
        this.cookieName = audience.cookieName();
    }

    @Override
    public String resolve(HttpServletRequest request) {
        // 만료된 쿠키가 남아 있어도 로그인·갱신·로그아웃은 되어야 한다
        String uri = request.getRequestURI();
        if (uri.endsWith("/auth/login") || uri.endsWith("/auth/refresh") || uri.endsWith("/auth/logout")) {
            return null;
        }
        String header = headerResolver.resolve(request);
        if (header != null) {
            return header;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName()) && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
