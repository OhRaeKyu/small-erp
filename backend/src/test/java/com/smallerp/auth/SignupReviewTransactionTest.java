package com.smallerp.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.smallerp.account.IndustryPackApplier;
import com.smallerp.account.TenantRepository;
import com.smallerp.account.UserRepository;
import com.smallerp.admin.PlatformAdmin;
import com.smallerp.admin.PlatformAdminRepository;
import com.smallerp.admin.PlatformAdminRole;
import com.smallerp.common.ApiException;
import com.smallerp.mail.MailSender;
import com.smallerp.signup.SignupRequest;
import com.smallerp.signup.SignupRequestRepository;
import com.smallerp.signup.SignupReviewService;
import com.smallerp.signup.SignupStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/** 실제 커밋·롤백을 확인해야 해서 테스트 트랜잭션 없이 돌리고, 끝나면 테이블을 비운다. */
@SpringBootTest
@ActiveProfiles("test")
class SignupReviewTransactionTest {

    @Autowired
    SignupReviewService reviewService;
    @Autowired
    SignupRequestRepository signupRequests;
    @Autowired
    TenantRepository tenants;
    @Autowired
    UserRepository users;
    @Autowired
    PlatformAdminRepository admins;
    @Autowired
    JdbcTemplate jdbc;

    @MockitoSpyBean
    IndustryPackApplier industryPackApplier;
    @MockitoSpyBean
    MailSender mailSender;

    Long adminId;
    Long requestId;

    @BeforeEach
    void setUp() {
        adminId = admins.save(new PlatformAdmin("tx-admin@test.com", "x", "운영자", PlatformAdminRole.SUPER)).getId();
        requestId = signupRequests.save(new SignupRequest("2148700124", "소금빵연구소", "이소금", "BAKERY_DESSERT",
                "010-1111-2222", "salt@bread.test", "hash")).getId();
    }

    @AfterEach
    void cleanUp() {
        jdbc.execute("delete from admin_audit_logs");
        jdbc.execute("delete from memberships");
        jdbc.execute("delete from signup_requests");
        jdbc.execute("delete from users");
        jdbc.execute("delete from tenants");
        jdbc.execute("delete from platform_admins");
    }

    @Test
    void 승인_중_하나라도_실패하면_아무것도_생성되지_않는다() {
        doThrow(new IllegalStateException("업종 팩 적용 실패")).when(industryPackApplier).apply(any());

        assertThatThrownBy(() -> reviewService.approve(requestId, adminId)).isInstanceOf(IllegalStateException.class);

        assertThat(tenants.count()).isZero();
        assertThat(users.count()).isZero();
        assertThat(jdbc.queryForObject("select count(*) from admin_audit_logs", Long.class)).isZero();
        assertThat(signupRequests.findById(requestId).orElseThrow().getStatus()).isEqualTo(SignupStatus.PENDING);
        verify(mailSender, timeout(300).times(0)).send(any());
    }

    @Test
    void 메일_발송이_실패해도_승인은_유지된다() {
        doThrow(new RuntimeException("SMTP 연결 실패")).when(mailSender).send(any());

        reviewService.approve(requestId, adminId);

        verify(mailSender, timeout(3000)).send(any());
        assertThat(signupRequests.findById(requestId).orElseThrow().getStatus()).isEqualTo(SignupStatus.APPROVED);
        assertThat(tenants.existsByBusinessNo("2148700124")).isTrue();
    }

    @Test
    void 두_관리자가_동시에_승인해도_한_번만_처리된다() throws Exception {
        int n = 4;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<String>> results = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Callable<String> task = () -> {
                start.await();
                try {
                    reviewService.approve(requestId, adminId);
                    return "OK";
                } catch (ApiException e) {
                    return e.code().name();
                }
            };
            results.add(pool.submit(task));
        }
        start.countDown();
        List<String> outcomes = new ArrayList<>();
        for (Future<String> f : results) {
            outcomes.add(f.get());
        }
        pool.shutdown();

        assertThat(outcomes).containsOnlyOnce("OK");
        assertThat(outcomes).filteredOn(o -> !o.equals("OK")).containsOnly("ALREADY_REVIEWED");
        assertThat(tenants.count()).isEqualTo(1);
    }
}
