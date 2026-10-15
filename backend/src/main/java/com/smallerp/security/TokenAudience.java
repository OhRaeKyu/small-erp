package com.smallerp.security;

/**
 * 토큰이 쓰일 서비스. 관리자 토큰으로 사용자 API를, 사용자 토큰으로 관리자 API를 호출할 수 없도록
 * aud 클레임과 쿠키 이름을 서비스별로 나눈다.
 */
public enum TokenAudience {
    ADMIN("admin", "ADMIN_TOKEN", "ADMIN_REFRESH", "/api/admin/auth"),
    APP("app", "APP_TOKEN", "APP_REFRESH", "/api/app/auth");

    private final String value;
    private final String cookieName;
    private final String refreshCookieName;
    private final String authPath;

    TokenAudience(String value, String cookieName, String refreshCookieName, String authPath) {
        this.value = value;
        this.cookieName = cookieName;
        this.refreshCookieName = refreshCookieName;
        this.authPath = authPath;
    }

    public String value() {
        return value;
    }

    public String cookieName() {
        return cookieName;
    }

    public String refreshCookieName() {
        return refreshCookieName;
    }

    /** 리프레시 쿠키는 이 경로(로그인·갱신·로그아웃)로만 전송된다 */
    public String authPath() {
        return authPath;
    }
}
