package com.smallerp.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** 실패 기록은 별도 트랜잭션으로 커밋되므로 테스트 트랜잭션 없이 돌리고, 끝나면 테이블을 비운다. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoginLockoutTest {

    static final String PASSWORD = "test1234!";
    static final String BUSINESS_NO = "2148700124";
    static final String ADMIN_EMAIL = "lock-admin@test.com";

    @Autowired
    MockMvc mvc;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    TenantRepository tenants;
    @Autowired
    UserRepository users;
    @Autowired
    MembershipRepository memberships;
    @Autowired
    PlatformAdminRepository admins;
    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        Tenant tenant = tenants.save(new Tenant(BUSINESS_NO, "소금빵연구소", "BAKERY_DESSERT"));
        User owner = users.save(new User(BUSINESS_NO, "owner@lock.test", passwordEncoder.encode(PASSWORD), "대표"));
        memberships.save(new Membership(tenant, owner, MembershipRole.OWNER));
        admins.save(new PlatformAdmin(ADMIN_EMAIL, passwordEncoder.encode(PASSWORD), "운영자", PlatformAdminRole.SUPER));
    }

    @AfterEach
    void cleanUp() {
        jdbc.execute("delete from memberships");
        jdbc.execute("delete from users");
        jdbc.execute("delete from tenants");
        jdbc.execute("delete from platform_admins");
    }

    @Test
    void 사용자는_5번_틀리면_맞는_비밀번호로도_로그인할_수_없다() throws Exception {
        for (int i = 0; i < 4; i++) {
            appLogin("wrong-pass").andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }
        appLogin("wrong-pass").andExpect(status().isUnauthorized());

        appLogin(PASSWORD).andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("LOGIN_LOCKED"));
    }

    @Test
    void 잠금_시간이_지나면_다시_로그인할_수_있고_횟수가_초기화된다() throws Exception {
        for (int i = 0; i < 5; i++) {
            appLogin("wrong-pass");
        }
        appLogin(PASSWORD).andExpect(status().isTooManyRequests());

        jdbc.update("update users set locked_until = now() - interval '1 second' where login_id = ?", BUSINESS_NO);

        appLogin(PASSWORD).andExpect(status().isOk());
        assertThat(userColumn("failed_login_count")).isEqualTo(0);
        assertThat(userColumn("locked_until")).isNull();
    }

    @Test
    void 로그인에_성공하면_실패_횟수가_초기화된다() throws Exception {
        for (int i = 0; i < 4; i++) {
            appLogin("wrong-pass");
        }
        assertThat(userColumn("failed_login_count")).isEqualTo(4);

        appLogin(PASSWORD).andExpect(status().isOk());
        assertThat(userColumn("failed_login_count")).isEqualTo(0);

        // 초기화됐으니 다시 4번 틀려도 잠기지 않는다
        for (int i = 0; i < 4; i++) {
            appLogin("wrong-pass").andExpect(status().isUnauthorized());
        }
        appLogin(PASSWORD).andExpect(status().isOk());
    }

    @Test
    void 관리자도_5번_틀리면_잠긴다() throws Exception {
        for (int i = 0; i < 5; i++) {
            adminLogin("wrong-pass").andExpect(status().isUnauthorized());
        }
        adminLogin(PASSWORD).andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("LOGIN_LOCKED"));
        // 사용자 계정에는 영향이 없다
        appLogin(PASSWORD).andExpect(status().isOk());
    }

    private Object userColumn(String column) {
        return jdbc.queryForObject("select " + column + " from users where login_id = ?", Object.class, BUSINESS_NO);
    }

    private ResultActions appLogin(String password) throws Exception {
        return mvc.perform(post("/api/app/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"loginId\":\"" + BUSINESS_NO + "\",\"password\":\"" + password + "\"}"));
    }

    private ResultActions adminLogin(String password) throws Exception {
        return mvc.perform(post("/api/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + ADMIN_EMAIL + "\",\"password\":\"" + password + "\"}"));
    }
}
