package com.smallerp.tenant;

import com.smallerp.account.BusinessHour;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 설정 > 사업장 정보 (U-080). 사업장은 토큰에서만 정한다 */
@RestController
@RequestMapping("/api/app/tenant")
public class TenantSettingsController {

    private final TenantSettingsService service;

    public TenantSettingsController(TenantSettingsService service) {
        this.service = service;
    }

    public record UpdateRequest(
            @NotBlank(message = "상호를 입력해 주세요.") @Size(max = 100, message = "상호는 100자 이하로 입력해 주세요.") String name,
            @Size(max = 200, message = "주소는 200자 이하로 입력해 주세요.") String address,
            @Pattern(regexp = "^$|^[0-9-]{8,20}$", message = "연락처를 확인해 주세요.") String phone,
            @NotNull(message = "영업시간을 입력해 주세요.") @Size(max = 7, message = "요일을 확인해 주세요.") List<BusinessHour> businessHours,
            @Min(value = 1, message = "1 이상으로 입력해 주세요.") @Max(value = 100000, message = "100000 이하로 입력해 주세요.") Integer defaultDailyCapacity,
            @Min(value = 0, message = "0 이상으로 입력해 주세요.") @Max(value = 60, message = "60일 이하로 입력해 주세요.") Integer orderLeadDays) {
    }

    @GetMapping
    public TenantProfile get(@AuthenticationPrincipal Jwt jwt) {
        return service.get(tenantId(jwt));
    }

    @PutMapping
    public TenantProfile update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateRequest req) {
        return service.update(tenantId(jwt), req);
    }

    private static Long tenantId(Jwt jwt) {
        return ((Number) jwt.getClaim("tenantId")).longValue();
    }
}
