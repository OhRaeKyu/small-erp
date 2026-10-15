package com.smallerp.signup;

import com.smallerp.account.IndustryPack;
import com.smallerp.account.IndustryPackRepository;
import com.smallerp.account.TenantRepository;
import com.smallerp.common.ApiException;
import com.smallerp.common.BusinessNumber;
import com.smallerp.common.ErrorCode;
import com.smallerp.storage.FileStorage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 회원가입 요청 (U-AUTH-01, 01a, 01b), 요청 상태 조회 (U-AUTH-01c, 01d) */
@RestController
@RequestMapping("/api/public")
public class SignupApplyController {

    private final SignupRequestRepository signupRequests;
    private final TenantRepository tenants;
    private final IndustryPackRepository industryPacks;
    private final PasswordEncoder passwordEncoder;
    private final FileStorage fileStorage;

    public SignupApplyController(SignupRequestRepository signupRequests, TenantRepository tenants,
                                 IndustryPackRepository industryPacks, PasswordEncoder passwordEncoder,
                                 FileStorage fileStorage) {
        this.signupRequests = signupRequests;
        this.tenants = tenants;
        this.industryPacks = industryPacks;
        this.passwordEncoder = passwordEncoder;
        this.fileStorage = fileStorage;
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

    public record StatusRequest(
            @NotBlank(message = "사업자등록번호를 입력해 주세요.") String businessNo,
            @NotBlank(message = "비밀번호를 입력해 주세요.") String password) {
    }

    /** 비밀번호가 맞을 때만 내려준다. 반려 시 다시 신청할 때 쓰도록 입력값도 함께 준다(비밀번호 제외). */
    public record StatusResponse(SignupStatus status, String businessNo, String companyName, String ownerName,
                                 String industryCode, String phone, String email, String rejectReason,
                                 Instant createdAt, Instant reviewedAt) {
    }

    public record IndustryOption(String code, String name) {
    }

    @GetMapping("/industry-packs")
    @Transactional(readOnly = true)
    public List<IndustryOption> industryPacks() {
        // "기타 제조"는 맨 뒤에
        return industryPacks.findAll(Sort.by("code")).stream()
                .sorted(Comparator.comparing((IndustryPack p) -> p.getCode().equals("GENERAL")))
                .map(p -> new IndustryOption(p.getCode(), p.getName()))
                .toList();
    }

    @PostMapping(path = "/signup-requests", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ApplyResponse apply(@Valid @RequestBody ApplyRequest req) {
        return new ApplyResponse(create(req, null).getId(), SignupStatus.PENDING);
    }

    /** 사업자등록증을 함께 올릴 때: request(JSON) + certificate(파일) 파트 */
    @PostMapping(path = "/signup-requests", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ApplyResponse applyWithCertificate(@Valid @RequestPart("request") ApplyRequest req,
                                              @RequestPart(value = "certificate", required = false)
                                              MultipartFile certificate) {
        return new ApplyResponse(create(req, certificate).getId(), SignupStatus.PENDING);
    }

    @PostMapping("/signup-requests/status")
    @Transactional(readOnly = true)
    public StatusResponse status(@Valid @RequestBody StatusRequest req) {
        String businessNo = BusinessNumber.normalize(req.businessNo());
        SignupRequest r = (businessNo == null ? null
                : signupRequests.findFirstByBusinessNoOrderByIdDesc(businessNo)
                        .filter(found -> passwordEncoder.matches(req.password(), found.getPasswordHash()))
                        .orElse(null));
        if (r == null) {
            throw new ApiException(ErrorCode.SIGNUP_NOT_FOUND);
        }
        return new StatusResponse(r.getStatus(), BusinessNumber.format(r.getBusinessNo()), r.getCompanyName(),
                r.getOwnerName(), r.getIndustryCode(), r.getPhone(), r.getEmail(), r.getRejectReason(),
                r.getCreatedAt(), r.getReviewedAt());
    }

    private SignupRequest create(ApplyRequest req, MultipartFile certificate) {
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
        // 파일 검사는 저장 전에 끝낸다
        byte[] content = null;
        CertificateFile type = null;
        if (certificate != null && !certificate.isEmpty()) {
            content = CertificateFile.read(certificate);
            type = CertificateFile.detect(content);
        }
        SignupRequest request = new SignupRequest(businessNo, req.companyName().trim(), req.ownerName().trim(),
                req.industryCode(), req.phone(), req.email().trim().toLowerCase(),
                passwordEncoder.encode(req.password()));
        if (content != null) {
            request.attachCertificate(fileStorage.save("certificates", type.extension(), content));
        }
        return signupRequests.saveAndFlush(request);
    }
}
