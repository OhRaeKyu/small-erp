package com.smallerp.signup;

import com.smallerp.account.IndustryPackApplier;
import com.smallerp.account.Membership;
import com.smallerp.account.MembershipRepository;
import com.smallerp.account.MembershipRole;
import com.smallerp.account.Tenant;
import com.smallerp.account.TenantRepository;
import com.smallerp.account.User;
import com.smallerp.account.UserRepository;
import com.smallerp.audit.AdminAuditLog;
import com.smallerp.audit.AdminAuditLogRepository;
import com.smallerp.common.ApiException;
import com.smallerp.common.BusinessNumber;
import com.smallerp.common.ErrorCode;
import com.smallerp.mail.MailMessage;
import com.smallerp.mail.MailRequestedEvent;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 회원가입 승인·반려 (A-SIGNUP-03, 04) */
@Service
public class SignupReviewService {

    static final String TARGET_TYPE = "SIGNUP_REQUEST";

    private final SignupRequestRepository signupRequests;
    private final TenantRepository tenants;
    private final UserRepository users;
    private final MembershipRepository memberships;
    private final IndustryPackApplier industryPackApplier;
    private final AdminAuditLogRepository auditLogs;
    private final ApplicationEventPublisher events;

    public SignupReviewService(SignupRequestRepository signupRequests, TenantRepository tenants,
                               UserRepository users, MembershipRepository memberships,
                               IndustryPackApplier industryPackApplier, AdminAuditLogRepository auditLogs,
                               ApplicationEventPublisher events) {
        this.signupRequests = signupRequests;
        this.tenants = tenants;
        this.users = users;
        this.memberships = memberships;
        this.industryPackApplier = industryPackApplier;
        this.auditLogs = auditLogs;
        this.events = events;
    }

    /**
     * 한 트랜잭션: 사업장 → 사업자 계정(login_id = 사업자번호) → 소속 → 업종 팩 적용 → 요청 승인 → 작업 이력.
     * 하나라도 실패하면 전부 롤백된다. 안내 메일은 커밋 후 비동기로 보낸다.
     */
    @Transactional
    public SignupRequest approve(Long requestId, Long adminId) {
        SignupRequest request = lockPending(requestId);
        if (tenants.existsByBusinessNo(request.getBusinessNo())
                || users.findByLoginIdIgnoreCase(request.getBusinessNo()).isPresent()) {
            throw new ApiException(ErrorCode.ALREADY_REGISTERED);
        }

        Tenant tenant = tenants.save(new Tenant(request.getBusinessNo(), request.getCompanyName(),
                request.getIndustryCode()));
        // 신청 때 받은 비밀번호 해시를 그대로 계정 비밀번호로 쓴다
        User user = users.save(new User(request.getBusinessNo(), request.getEmail(), request.getPasswordHash(),
                request.getOwnerName()));
        memberships.save(new Membership(tenant, user, MembershipRole.OWNER));
        industryPackApplier.apply(tenant);
        request.markApproved(adminId, tenant.getId());
        auditLogs.save(new AdminAuditLog(adminId, AdminAuditLog.Action.APPROVE, TARGET_TYPE, request.getId(),
                Map.of("businessNo", request.getBusinessNo(), "tenantId", tenant.getId())));

        events.publishEvent(new MailRequestedEvent(new MailMessage(request.getEmail(),
                "[나의 작은 ERP] 회원가입이 승인되었습니다",
                """
                %s 님, 나의 작은 ERP 회원가입이 승인되었습니다.

                사업자등록번호 %s 와 가입할 때 정한 비밀번호로 로그인해 주세요.
                """.formatted(request.getOwnerName(), BusinessNumber.format(request.getBusinessNo())))));
        return request;
    }

    @Transactional
    public SignupRequest reject(Long requestId, Long adminId, String reason) {
        SignupRequest request = lockPending(requestId);
        request.markRejected(adminId, reason);
        auditLogs.save(new AdminAuditLog(adminId, AdminAuditLog.Action.REJECT, TARGET_TYPE, request.getId(),
                Map.of("businessNo", request.getBusinessNo(), "reason", reason)));

        events.publishEvent(new MailRequestedEvent(new MailMessage(request.getEmail(),
                "[나의 작은 ERP] 회원가입이 반려되었습니다",
                """
                %s 님, 나의 작은 ERP 회원가입 요청이 반려되었습니다.

                사유: %s

                내용을 고쳐 다시 회원가입할 수 있습니다.
                """.formatted(request.getOwnerName(), reason))));
        return request;
    }

    private SignupRequest lockPending(Long requestId) {
        SignupRequest request = signupRequests.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (request.getStatus() != SignupStatus.PENDING) {
            throw new ApiException(ErrorCode.ALREADY_REVIEWED);
        }
        return request;
    }
}
