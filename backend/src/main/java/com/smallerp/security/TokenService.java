package com.smallerp.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.smallerp.common.ApiException;
import com.smallerp.common.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

/**
 * 액세스 토큰(짧게)과 리프레시 토큰(길게)을 발급·검증한다. 둘 다 httpOnly 쿠키로 내려준다.
 * 리프레시 토큰은 서버에 저장하지 않는다(무상태). 갱신할 때 계정·사업장 상태를 다시 확인한다.
 */
@Component
public class TokenService {

    static final String ISSUER = "small-erp";
    private static final String TYPE_CLAIM = "typ";
    private static final String ACCESS = "access";
    private static final String REFRESH = "refresh";

    private final SecretKey key;
    private final JwtEncoder encoder;
    private final Duration accessTtl;
    private final Duration refreshTtl;
    private final boolean secureCookie;

    public TokenService(AppProperties props) {
        byte[] secret = props.jwt().secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("app.jwt.secret은 32바이트 이상이어야 합니다.");
        }
        this.key = new SecretKeySpec(secret, "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        this.accessTtl = props.jwt().accessTokenTtl();
        this.refreshTtl = props.jwt().refreshTokenTtl();
        this.secureCookie = props.cookie().secure();
    }

    /** 로그인·갱신 시 내려줄 Set-Cookie 헤더 (액세스 + 리프레시) */
    public HttpHeaders loginCookies(TokenAudience audience, String subject, Map<String, Object> claims) {
        String access = encode(audience, subject, ACCESS, accessTtl, claims);
        String refresh = encode(audience, subject, REFRESH, refreshTtl, Map.of());
        HttpHeaders headers = new HttpHeaders();
        // 액세스 쿠키 자체는 리프레시 기간만큼 둔다. 안의 토큰이 만료되면 프론트가 갱신을 요청한다.
        headers.add(HttpHeaders.SET_COOKIE, cookie(audience.cookieName(), access, "/", refreshTtl).toString());
        headers.add(HttpHeaders.SET_COOKIE,
                cookie(audience.refreshCookieName(), refresh, audience.authPath(), refreshTtl).toString());
        return headers;
    }

    public HttpHeaders logoutCookies(TokenAudience audience) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, cookie(audience.cookieName(), "", "/", Duration.ZERO).toString());
        headers.add(HttpHeaders.SET_COOKIE,
                cookie(audience.refreshCookieName(), "", audience.authPath(), Duration.ZERO).toString());
        return headers;
    }

    /** API 인증용 디코더: 지정한 서비스(aud)의 액세스 토큰만 통과시킨다 */
    public JwtDecoder accessDecoderFor(TokenAudience audience) {
        return decoder(audience, ACCESS);
    }

    /** 리프레시 토큰을 검증하고 subject를 돌려준다 */
    public String verifyRefresh(TokenAudience audience, String token) {
        if (token == null || token.isBlank()) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
        try {
            Jwt jwt = decoder(audience, REFRESH).decode(token);
            return jwt.getSubject();
        } catch (JwtException e) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
    }

    private String encode(TokenAudience audience, String subject, String type, Duration ttl,
                          Map<String, Object> claims) {
        Instant now = Instant.now();
        JwtClaimsSet set = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .audience(List.of(audience.value()))
                .subject(subject)
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .claim(TYPE_CLAIM, type)
                .claims(c -> c.putAll(claims))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, set)).getTokenValue();
    }

    private JwtDecoder decoder(TokenAudience audience, String type) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<Jwt> audAndType = jwt ->
                jwt.getAudience() != null && jwt.getAudience().contains(audience.value())
                        && type.equals(jwt.getClaimAsString(TYPE_CLAIM))
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(
                                new OAuth2Error("invalid_token", "audience or type mismatch", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(ISSUER), audAndType));
        return decoder;
    }

    private ResponseCookie cookie(String name, String value, String path, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path(path)
                .maxAge(maxAge)
                .build();
    }
}
