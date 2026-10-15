package com.smallerp.seed;

import com.smallerp.account.Membership;
import com.smallerp.account.MembershipRepository;
import com.smallerp.account.MembershipRole;
import com.smallerp.account.Tenant;
import com.smallerp.account.TenantRepository;
import com.smallerp.account.User;
import com.smallerp.account.UserRepository;
import com.smallerp.admin.PlatformAdmin;
import com.smallerp.admin.PlatformAdminRepository;
import com.smallerp.admin.PlatformAdminRole;
import com.smallerp.security.AppProperties;
import com.smallerp.signup.SignupRequest;
import com.smallerp.signup.SignupRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로컬 개발용 샘플 데이터 (local 프로필). 관리자가 하나도 없을 때만 넣는다.
 * 사업자 비밀번호는 모두 test1234!
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class LocalDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LocalDataSeeder.class);
    private static final String SAMPLE_PASSWORD = "test1234!";

    private final AppProperties props;
    private final PlatformAdminRepository admins;
    private final SignupRequestRepository signupRequests;
    private final TenantRepository tenants;
    private final UserRepository users;
    private final MembershipRepository memberships;
    private final PasswordEncoder passwordEncoder;

    public LocalDataSeeder(AppProperties props, PlatformAdminRepository admins,
                           SignupRequestRepository signupRequests, TenantRepository tenants,
                           UserRepository users, MembershipRepository memberships,
                           PasswordEncoder passwordEncoder) {
        this.props = props;
        this.admins = admins;
        this.signupRequests = signupRequests;
        this.tenants = tenants;
        this.users = users;
        this.memberships = memberships;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (admins.count() > 0) {
            return;
        }
        PlatformAdmin admin = admins.save(new PlatformAdmin(props.seed().adminEmail(),
                passwordEncoder.encode(props.seed().adminPassword()), "운영자", PlatformAdminRole.SUPER));
        String pw = passwordEncoder.encode(SAMPLE_PASSWORD);

        // 승인 완료: 대표 계정으로 로그인할 수 있는 테스트 사업장
        SignupRequest approved = signupRequests.save(new SignupRequest("1234567891", "달콤케이크 공방", "김달콤",
                "BAKERY_DESSERT", "010-1234-5678", "owner@dalkom.test", pw));
        Tenant tenant = tenants.save(new Tenant("1234567891", "달콤케이크 공방", "BAKERY_DESSERT"));
        User owner = users.save(new User("1234567891", "owner@dalkom.test", pw, "김달콤"));
        memberships.save(new Membership(tenant, owner, MembershipRole.OWNER));
        approved.markApproved(admin.getId(), tenant.getId());

        // 승인 대기
        signupRequests.save(new SignupRequest("2148700124", "소금빵연구소", "이소금",
                "BAKERY_DESSERT", "010-2222-3333", "salt@bread.test", pw));
        signupRequests.save(new SignupRequest("1058123457", "할매수제청", "박할매",
                "HANDMADE_FOOD", "010-4444-5555", "cheong@jam.test", pw));
        signupRequests.save(new SignupRequest("2208111115", "나무결 공방", "최나무",
                "HANDMADE_CRAFT", "010-6666-7777", "wood@craft.test", pw));
        signupRequests.save(new SignupRequest("3141592650", "파이앤타르트", "정파이",
                "BAKERY_DESSERT", "02-123-4567", "pie@tart.test", pw));

        // 반려
        SignupRequest rejected = signupRequests.save(new SignupRequest("2718281824", "오일러 반찬가게", "오일러",
                "HANDMADE_FOOD", "010-8888-9999", "banchan@food.test", pw));
        rejected.markRejected(admin.getId(), "사업자등록증 상호와 신청 상호가 다릅니다.");

        log.info("로컬 샘플 데이터 생성: 관리자 {} / 대표 로그인 123-45-67891 (비밀번호 {})",
                props.seed().adminEmail(), SAMPLE_PASSWORD);
    }
}
