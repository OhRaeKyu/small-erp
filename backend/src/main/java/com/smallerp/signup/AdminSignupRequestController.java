package com.smallerp.signup;

import com.smallerp.account.IndustryPackRepository;
import com.smallerp.common.BusinessNumber;
import com.smallerp.common.PageResponse;
import com.smallerp.admin.PlatformAdminRepository;
import com.smallerp.audit.AdminAuditLog;
import com.smallerp.audit.AdminAuditLogRepository;
import com.smallerp.common.ApiException;
import com.smallerp.common.ErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 회원가입 요청 목록·상세·승인·반려 (A-SIGNUP-01~04, 화면 A-015·016) */
@RestController
@RequestMapping("/api/admin/signup-requests")
public class AdminSignupRequestController {

    private static final int MAX_PAGE_SIZE = 100;

    private final SignupRequestRepository signupRequests;
    private final IndustryPackRepository industryPacks;
    private final PlatformAdminRepository admins;
    private final AdminAuditLogRepository auditLogs;
    private final SignupReviewService reviewService;

    public AdminSignupRequestController(SignupRequestRepository signupRequests,
                                        IndustryPackRepository industryPacks, PlatformAdminRepository admins,
                                        AdminAuditLogRepository auditLogs, SignupReviewService reviewService) {
        this.signupRequests = signupRequests;
        this.industryPacks = industryPacks;
        this.admins = admins;
        this.auditLogs = auditLogs;
        this.reviewService = reviewService;
    }

    public enum SortOrder {
        LATEST,
        OLDEST
    }

    public record SignupRequestRow(Long id, String businessNo, String companyName, String ownerName,
                                   String industryCode, String industryName, String phone, String email,
                                   SignupStatus status, boolean hasCertificate, Instant createdAt,
                                   Instant reviewedAt) {
    }

    /**
     * 기본은 대기 중 신청을 오래된 순으로 (먼저 온 신청부터 처리). 상태 기본값은 화면에서 정한다.
     *
     * @param status 없으면 전체
     * @param q      사업자번호(하이픈 무관)·상호·대표자명 검색
     */
    @GetMapping
    @Transactional(readOnly = true)
    public PageResponse<SignupRequestRow> list(@RequestParam(required = false) SignupStatus status,
                                               @RequestParam(required = false) String q,
                                               @RequestParam(defaultValue = "OLDEST") SortOrder sort,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        Sort order = sort == SortOrder.OLDEST
                ? Sort.by("createdAt").ascending().and(Sort.by("id").ascending())
                : Sort.by("createdAt").descending().and(Sort.by("id").descending());
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), order);
        Map<String, String> industryNames = industryPacks.namesByCode();
        return PageResponse.of(signupRequests.findAll(filter(status, q), pageable), r -> toRow(r, industryNames));
    }

    public record SignupRequestDetail(SignupRequestRow request, String rejectReason, String reviewerName,
                                      Long tenantId, List<SignupRequestRow> history, List<AuditEntry> auditLogs) {
    }

    public record AuditEntry(AdminAuditLog.Action action, String adminName, Instant createdAt) {
    }

    public record RejectRequest(
            @NotBlank(message = "반려 사유를 입력해 주세요.")
            @Size(min = 5, max = 500, message = "반려 사유는 5자 이상 500자 이하로 입력해 주세요.") String reason) {
    }

    /** 상세: 입력 정보, 같은 사업자번호의 다른 요청 이력, 작업 이력 */
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public SignupRequestDetail detail(@PathVariable Long id) {
        SignupRequest r = signupRequests.findById(id).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        return toDetail(r);
    }

    @PostMapping("/{id}/approve")
    public SignupRequestDetail approve(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        reviewService.approve(id, Long.valueOf(jwt.getSubject()));
        return detail(id);
    }

    @PostMapping("/{id}/reject")
    public SignupRequestDetail reject(@PathVariable Long id, @Valid @RequestBody RejectRequest req,
                                      @AuthenticationPrincipal Jwt jwt) {
        reviewService.reject(id, Long.valueOf(jwt.getSubject()), req.reason().trim());
        return detail(id);
    }

    private SignupRequestDetail toDetail(SignupRequest r) {
        Map<String, String> industryNames = industryPacks.namesByCode();
        Map<Long, String> adminNames = new HashMap<>();
        admins.findAll().forEach(a -> adminNames.put(a.getId(), a.getName()));
        List<SignupRequestRow> history = signupRequests.findByBusinessNoOrderByIdDesc(r.getBusinessNo()).stream()
                .filter(h -> !h.getId().equals(r.getId()))
                .map(h -> toRow(h, industryNames))
                .toList();
        List<AuditEntry> audits = auditLogs.findByTargetTypeAndTargetIdOrderByIdDesc(
                        SignupReviewService.TARGET_TYPE, r.getId()).stream()
                .map(a -> new AuditEntry(a.getAction(), adminNames.get(a.getAdminId()), a.getCreatedAt()))
                .toList();
        return new SignupRequestDetail(toRow(r, industryNames), r.getRejectReason(),
                r.getReviewedBy() == null ? null : adminNames.get(r.getReviewedBy()), r.getTenantId(), history, audits);
    }

    private static SignupRequestRow toRow(SignupRequest r, Map<String, String> industryNames) {
        return new SignupRequestRow(r.getId(), BusinessNumber.format(r.getBusinessNo()), r.getCompanyName(),
                r.getOwnerName(), r.getIndustryCode(),
                industryNames.getOrDefault(r.getIndustryCode(), r.getIndustryCode()), r.getPhone(), r.getEmail(),
                r.getStatus(), r.getCertificateFileKey() != null, r.getCreatedAt(), r.getReviewedAt());
    }

    /** 상태 필터 탭에 보여줄 건수 */
    @GetMapping("/counts")
    @Transactional(readOnly = true)
    public Map<SignupStatus, Long> counts() {
        Map<SignupStatus, Long> counts = new EnumMap<>(SignupStatus.class);
        for (SignupStatus s : SignupStatus.values()) {
            counts.put(s, 0L);
        }
        signupRequests.countByStatus().forEach(c -> counts.put(c.getStatus(), c.getCount()));
        return counts;
    }

    private static Specification<SignupRequest> filter(SignupStatus status, String q) {
        Specification<SignupRequest> spec = (root, query, cb) -> cb.conjunction();
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (StringUtils.hasText(q)) {
            String keyword = q.trim();
            String digits = keyword.replace("-", "");
            String like = "%" + escapeLike(keyword.toLowerCase()) + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    digits.matches("\\d+") ? cb.like(root.get("businessNo"), "%" + digits + "%") : cb.disjunction(),
                    cb.like(cb.lower(root.get("companyName")), like, '\\'),
                    cb.like(cb.lower(root.get("ownerName")), like, '\\')));
        }
        return spec;
    }

    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
