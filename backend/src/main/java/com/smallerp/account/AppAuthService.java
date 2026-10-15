package com.smallerp.account;

import com.smallerp.common.ApiException;
import com.smallerp.common.BusinessNumber;
import com.smallerp.common.ErrorCode;
import com.smallerp.login.LoginAttemptService;
import com.smallerp.signup.SignupRequest;
import com.smallerp.signup.SignupRequestRepository;
import com.smallerp.signup.SignupStatus;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppAuthService {

    private final UserRepository users;
    private final MembershipRepository memberships;
    private final SignupRequestRepository signupRequests;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttempts;

    public AppAuthService(UserRepository users, MembershipRepository memberships,
                          SignupRequestRepository signupRequests, PasswordEncoder passwordEncoder,
                          LoginAttemptService loginAttempts) {
        this.loginAttempts = loginAttempts;
        this.users = users;
        this.memberships = memberships;
        this.signupRequests = signupRequests;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 사업자번호(하이픈 있어도 됨)로 로그인한다. 이메일 로그인은 직원 계정(후순위)용으로 남겨 둔다.
     * 아직 계정이 없는 사업자번호라면 가입 신청 상태(대기·반려)를 알려 준다.
     */
    @Transactional(readOnly = true)
    public Membership login(String rawLoginId, String password) {
        String businessNo = BusinessNumber.normalize(rawLoginId);
        String loginId = businessNo != null ? businessNo : rawLoginId.trim();

        User user = users.findByLoginIdIgnoreCase(loginId).orElse(null);
        if (user == null) {
            if (businessNo != null) {
                throwIfSignupNotApproved(businessNo, password);
            }
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }
        loginAttempts.checkNotLocked(user.getLockedUntil());
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            loginAttempts.recordFailure(LoginAttemptService.Account.USER, user.getId());
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (user.getStatus() == UserStatus.LOCKED) {
            throw new ApiException(ErrorCode.ACCOUNT_LOCKED);
        }

        Membership membership = activeMembership(user.getId());
        loginAttempts.recordSuccess(LoginAttemptService.Account.USER, user.getId());
        return membership;
    }

    @Transactional(readOnly = true)
    public Membership refresh(Long userId) {
        User user = users.findById(userId).orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        if (user.getStatus() == UserStatus.LOCKED) {
            throw new ApiException(ErrorCode.ACCOUNT_LOCKED);
        }
        return activeMembership(userId);
    }

    // 여러 사업장 소속·전환(U-AUTH-05)은 후순위. 지금은 첫 소속 사업장으로 로그인한다.
    private Membership activeMembership(Long userId) {
        List<Membership> list = memberships.findWithTenantByUserId(userId);
        return list.stream()
                .filter(m -> m.getTenant().getStatus() == TenantStatus.ACTIVE)
                .findFirst()
                .orElseThrow(() -> new ApiException(
                        list.isEmpty() ? ErrorCode.NO_MEMBERSHIP : ErrorCode.TENANT_SUSPENDED));
    }

    private void throwIfSignupNotApproved(String businessNo, String password) {
        signupRequests.findFirstByBusinessNoOrderByIdDesc(businessNo)
                .filter(r -> passwordEncoder.matches(password, r.getPasswordHash()))
                .map(SignupRequest::getStatus)
                .ifPresent(status -> {
                    if (status == SignupStatus.PENDING) {
                        throw new ApiException(ErrorCode.SIGNUP_PENDING);
                    }
                    if (status == SignupStatus.REJECTED) {
                        throw new ApiException(ErrorCode.SIGNUP_REJECTED);
                    }
                });
    }

    @Transactional(readOnly = true)
    public Membership current(Long userId, Long tenantId) {
        return memberships.findWithTenantAndUser(userId, tenantId)
                .filter(m -> m.getTenant().getStatus() == TenantStatus.ACTIVE
                        && m.getUser().getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
    }
}
