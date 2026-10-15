package com.smallerp.signup;

import com.smallerp.account.IndustryPackRepository;
import com.smallerp.common.BusinessNumber;
import com.smallerp.common.PageResponse;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 가입 신청 목록 (A-SIGNUP-01, 화면 A-015) */
@RestController
@RequestMapping("/api/admin/signup-requests")
public class AdminSignupRequestController {

    private static final int MAX_PAGE_SIZE = 100;

    private final SignupRequestRepository signupRequests;
    private final IndustryPackRepository industryPacks;

    public AdminSignupRequestController(SignupRequestRepository signupRequests,
                                        IndustryPackRepository industryPacks) {
        this.signupRequests = signupRequests;
        this.industryPacks = industryPacks;
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
        return PageResponse.of(signupRequests.findAll(filter(status, q), pageable), r -> new SignupRequestRow(
                r.getId(), BusinessNumber.format(r.getBusinessNo()), r.getCompanyName(), r.getOwnerName(),
                r.getIndustryCode(), industryNames.getOrDefault(r.getIndustryCode(), r.getIndustryCode()),
                r.getPhone(), r.getEmail(), r.getStatus(), r.getCertificateFileKey() != null,
                r.getCreatedAt(), r.getReviewedAt()));
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
