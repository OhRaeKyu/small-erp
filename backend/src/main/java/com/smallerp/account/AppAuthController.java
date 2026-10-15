package com.smallerp.account;

import com.smallerp.common.BusinessNumber;
import com.smallerp.security.TokenAudience;
import com.smallerp.security.TokenService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/auth")
public class AppAuthController {

    private final AppAuthService authService;
    private final TokenService tokens;

    public AppAuthController(AppAuthService authService, TokenService tokens) {
        this.authService = authService;
        this.tokens = tokens;
    }

    public record LoginRequest(
            @NotBlank(message = "사업자등록번호 또는 이메일을 입력해 주세요.") String loginId,
            @NotBlank(message = "비밀번호를 입력해 주세요.") String password) {
    }

    public record AppMe(UserInfo user, TenantInfo tenant, MembershipRole role) {

        static AppMe of(Membership m) {
            User u = m.getUser();
            Tenant t = m.getTenant();
            return new AppMe(new UserInfo(u.getId(), u.getName(), u.getEmail()),
                    new TenantInfo(t.getId(), t.getName(), BusinessNumber.format(t.getBusinessNo()), t.getIndustryCode()),
                    m.getRole());
        }
    }

    public record UserInfo(Long id, String name, String email) {
    }

    public record TenantInfo(Long id, String name, String businessNo, String industryCode) {
    }

    @PostMapping("/login")
    public ResponseEntity<AppMe> login(@Valid @RequestBody LoginRequest req) {
        Membership m = authService.login(req.loginId(), req.password());
        return ResponseEntity.ok().headers(loginCookies(m)).body(AppMe.of(m));
    }

    /**
     * 액세스 토큰이 만료되면 리프레시 쿠키로 새로 받는다.
     * 그사이 계정이 잠기거나 사업장이 정지되었으면 거부한다.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AppMe> refresh(
            @CookieValue(name = "APP_REFRESH", required = false) String refreshToken) {
        Long userId = Long.valueOf(tokens.verifyRefresh(TokenAudience.APP, refreshToken));
        Membership m = authService.refresh(userId);
        return ResponseEntity.ok().headers(loginCookies(m)).body(AppMe.of(m));
    }

    private HttpHeaders loginCookies(Membership m) {
        // 사업장은 토큰에서만 가져온다 (요청 본문·URL의 사업장 id는 신뢰하지 않음)
        return tokens.loginCookies(TokenAudience.APP, m.getUser().getId().toString(),
                Map.of("tenantId", m.getTenant().getId(), "role", m.getRole().name()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().headers(tokens.logoutCookies(TokenAudience.APP)).build();
    }

    @GetMapping("/me")
    public AppMe me(@AuthenticationPrincipal Jwt jwt) {
        Long tenantId = ((Number) jwt.getClaim("tenantId")).longValue();
        return AppMe.of(authService.current(Long.valueOf(jwt.getSubject()), tenantId));
    }
}
