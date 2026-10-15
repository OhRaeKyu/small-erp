package com.smallerp.signup;

import com.smallerp.account.IndustryPackRepository;
import com.smallerp.account.TenantRepository;
import com.smallerp.common.ApiException;
import com.smallerp.common.BusinessNumber;
import com.smallerp.common.ErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 가입 신청 (U-AUTH-01, 01a). 신청 화면(U-002)은 다음 작업에서 붙인다. */
@RestController
@RequestMapping("/api/public/signup-requests")
public class SignupApplyController {

    private final SignupRequestRepository signupRequests;
    private final TenantRepository tenants;
    private final IndustryPackRepository industryPacks;
    private final PasswordEncoder passwordEncoder;

    public SignupApplyController(SignupRequestRepository signupRequests, TenantRepository tenants,
                                 IndustryPackRepository industryPacks, PasswordEncoder passwordEncoder) {
        this.signupRequests = signupRequests;
        this.tenants = tenants;
        this.industryPacks = industryPacks;
        this.passwordEncoder = passwordEncoder;
    }

    public record ApplyRequest(
            @NotBlank(message = "사업자등록번호를 입력해 주세요.") String businessNo,
            @NotBlank(message = "상호를 입력해 주세요.") @Size(max = 100) String companyName,
            @NotBlank(message = "대표자명을 입력해 주세요.") @Size(max = 50) String ownerName,
            @NotBlank(message = "업종을 선택해 주세요.") String industryCode,
            @NotBlank(message = "연락처를 입력해 주세요.")
            @Pattern(regexp = "^[0-9-]{9,13}$", message = "연락처 형식이 아닙니다.") String phone,
            @NotBlank(message = "이메일을 입력해 주세요.") @Email(message = "이메일 형식이 아닙니다.") String email,
            @NotBlank(message = "비밀번호를 입력해 주세요.")
            @Size(min = 8, max = 64, message = "비밀번호는 8자 이상이어야 합니다.")
            @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "비밀번호에 영문과 숫자를 모두 넣어 주세요.")
            String password) {
    }

    public record ApplyResponse(Long id, SignupStatus status) {
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ApplyResponse apply(@Valid @RequestBody ApplyRequest req) {
        if (!BusinessNumber.isValid(req.businessNo())) {
            throw new ApiException(ErrorCode.INVALID_BUSINESS_NO);
        }
        String businessNo = BusinessNumber.normalize(req.businessNo());
        if (!industryPacks.existsById(req.industryCode())) {
            throw new ApiException(ErrorCode.UNKNOWN_INDUSTRY);
        }
        if (tenants.existsByBusinessNo(businessNo)) {
            throw new ApiException(ErrorCode.ALREADY_REGISTERED);
        }
        if (signupRequests.existsByBusinessNoAndStatus(businessNo, SignupStatus.PENDING)) {
            throw new ApiException(ErrorCode.SIGNUP_ALREADY_PENDING);
        }
        SignupRequest saved = signupRequests.saveAndFlush(new SignupRequest(businessNo, req.companyName().trim(),
                req.ownerName().trim(), req.industryCode(), req.phone(), req.email().trim().toLowerCase(),
                passwordEncoder.encode(req.password())));
        return new ApplyResponse(saved.getId(), saved.getStatus());
    }
}
