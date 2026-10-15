package com.smallerp.admin;

import com.smallerp.common.ApiException;
import com.smallerp.common.ErrorCode;
import com.smallerp.security.TokenAudience;
import com.smallerp.security.TokenService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final PlatformAdminRepository admins;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;

    public AdminAuthController(PlatformAdminRepository admins, PasswordEncoder passwordEncoder, TokenService tokens) {
        this.admins = admins;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
    }

    public record LoginRequest(
            @NotBlank(message = "이메일을 입력해 주세요.") @Email(message = "이메일 형식이 아닙니다.") String email,
            @NotBlank(message = "비밀번호를 입력해 주세요.") String password) {
    }

    public record AdminMe(Long id, String email, String name, PlatformAdminRole role) {

        static AdminMe of(PlatformAdmin a) {
            return new AdminMe(a.getId(), a.getEmail(), a.getName(), a.getRole());
        }
    }

    @PostMapping("/login")
    @Transactional(readOnly = true)
    public ResponseEntity<AdminMe> login(@Valid @RequestBody LoginRequest req) {
        PlatformAdmin admin = admins.findByEmailIgnoreCase(req.email().trim())
                .filter(a -> passwordEncoder.matches(req.password(), a.getPasswordHash()))
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS));
        return ResponseEntity.ok().headers(loginCookies(admin)).body(AdminMe.of(admin));
    }

    /** 액세스 토큰이 만료되면 리프레시 쿠키로 새로 받는다 */
    @PostMapping("/refresh")
    @Transactional(readOnly = true)
    public ResponseEntity<AdminMe> refresh(
            @CookieValue(name = "ADMIN_REFRESH", required = false) String refreshToken) {
        String subject = tokens.verifyRefresh(TokenAudience.ADMIN, refreshToken);
        PlatformAdmin admin = admins.findById(Long.valueOf(subject))
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        return ResponseEntity.ok().headers(loginCookies(admin)).body(AdminMe.of(admin));
    }

    private HttpHeaders loginCookies(PlatformAdmin admin) {
        return tokens.loginCookies(TokenAudience.ADMIN, admin.getId().toString(),
                Map.of("role", admin.getRole().name()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().headers(tokens.logoutCookies(TokenAudience.ADMIN)).build();
    }

    @GetMapping("/me")
    @Transactional(readOnly = true)
    public AdminMe me(@AuthenticationPrincipal Jwt jwt) {
        return admins.findById(Long.valueOf(jwt.getSubject()))
                .map(AdminMe::of)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
    }
}
