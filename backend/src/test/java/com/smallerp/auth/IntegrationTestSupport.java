package com.smallerp.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.smallerp.signup.SignupRequest;
import com.smallerp.signup.SignupRequestRepository;
import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
abstract class IntegrationTestSupport {

    static final String PASSWORD = "test1234!";

    @Autowired
    MockMvc mvc;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    PlatformAdminRepository admins;
    @Autowired
    SignupRequestRepository signupRequests;
    @Autowired
    TenantRepository tenants;
    @Autowired
    UserRepository users;
    @Autowired
    MembershipRepository memberships;

    PlatformAdmin createAdmin(String email) {
        return admins.save(new PlatformAdmin(email, passwordEncoder.encode(PASSWORD), "운영자", PlatformAdminRole.SUPER));
    }

    Tenant createOwner(String businessNo, String name) {
        Tenant tenant = tenants.save(new Tenant(businessNo, name, "BAKERY_DESSERT"));
        User owner = users.save(new User(businessNo, businessNo + "@owner.test", passwordEncoder.encode(PASSWORD), "대표"));
        memberships.save(new Membership(tenant, owner, MembershipRole.OWNER));
        return tenant;
    }

    SignupRequest createSignup(String businessNo, String company) {
        return signupRequests.saveAndFlush(new SignupRequest(businessNo, company, "신청자", "BAKERY_DESSERT",
                "010-0000-0000", businessNo + "@apply.test", passwordEncoder.encode(PASSWORD)));
    }

    Cookie adminLogin(String email) throws Exception {
        return mvc.perform(post("/api/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("ADMIN_TOKEN");
    }

    Cookie appLogin(String loginId) throws Exception {
        return mvc.perform(post("/api/app/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"" + loginId + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("APP_TOKEN");
    }
}
