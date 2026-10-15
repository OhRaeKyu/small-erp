package com.smallerp.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.smallerp.audit.AdminAuditLog;
import com.smallerp.audit.AdminAuditLogRepository;
import jakarta.servlet.http.Cookie;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class SignupReviewTest extends IntegrationTestSupport {

    @Autowired
    AdminAuditLogRepository auditLogs;

    Cookie admin;

    @BeforeEach
    void setUp() throws Exception {
        createAdmin("admin@test.com");
        admin = adminLogin("admin@test.com");
    }

    private org.springframework.test.web.servlet.ResultActions reject(Long id, String reason) throws Exception {
        return mvc.perform(post("/api/admin/signup-requests/{id}/reject", id).cookie(admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"" + reason + "\"}"));
    }

    @Test
    void 승인하면_사업장_계정_소속_업종팩이_생기고_사업자번호로_로그인된다() throws Exception {
        var r = createSignup("2148700124", "소금빵연구소");

        mvc.perform(post("/api/admin/signup-requests/{id}/approve", r.getId()).cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.request.status").value("APPROVED"))
                .andExpect(jsonPath("$.reviewerName").value("운영자"))
                .andExpect(jsonPath("$.tenantId").isNumber());

        var tenant = tenants.findAll().stream().filter(t -> t.getBusinessNo().equals("2148700124")).findFirst().orElseThrow();
        assertThat(tenant.getName()).isEqualTo("소금빵연구소");
        assertThat(tenant.getSettings()).containsKey("itemCategories");
        assertThat(users.findByLoginIdIgnoreCase("2148700124")).isPresent();
        assertThat(memberships.findWithTenantByUserId(users.findByLoginIdIgnoreCase("2148700124").get().getId()))
                .hasSize(1);

        appLogin("214-87-00124");
    }

    @Test
    void 승인과_반려는_작업_이력에_남는다() throws Exception {
        var a = createSignup("2148700124", "소금빵연구소");
        var b = createSignup("1058123457", "할매수제청");
        mvc.perform(post("/api/admin/signup-requests/{id}/approve", a.getId()).cookie(admin)).andExpect(status().isOk());
        reject(b.getId(), "사업자등록증 상호가 다릅니다").andExpect(status().isOk());

        assertThat(auditLogs.findByTargetTypeAndTargetIdOrderByIdDesc("SIGNUP_REQUEST", a.getId()))
                .extracting(AdminAuditLog::getAction).containsExactly(AdminAuditLog.Action.APPROVE);
        List<AdminAuditLog> rejectLogs = auditLogs.findByTargetTypeAndTargetIdOrderByIdDesc("SIGNUP_REQUEST", b.getId());
        assertThat(rejectLogs).extracting(AdminAuditLog::getAction).containsExactly(AdminAuditLog.Action.REJECT);
        assertThat(rejectLogs.get(0).getDetail()).containsEntry("reason", "사업자등록증 상호가 다릅니다");

        mvc.perform(get("/api/admin/signup-requests/{id}", b.getId()).cookie(admin))
                .andExpect(jsonPath("$.rejectReason").value("사업자등록증 상호가 다릅니다"))
                .andExpect(jsonPath("$.auditLogs[0].action").value("REJECT"))
                .andExpect(jsonPath("$.auditLogs[0].adminName").value("운영자"));
    }

    @Test
    void 반려_사유가_없거나_5자_미만이면_거부된다() throws Exception {
        var r = createSignup("2148700124", "소금빵연구소");
        reject(r.getId(), "").andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.reason").exists());
        reject(r.getId(), "짧음").andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/signup-requests/{id}", r.getId()).cookie(admin))
                .andExpect(jsonPath("$.request.status").value("PENDING"));
    }

    @Test
    void 이미_처리된_요청은_다시_승인_반려할_수_없다() throws Exception {
        var r = createSignup("2148700124", "소금빵연구소");
        mvc.perform(post("/api/admin/signup-requests/{id}/approve", r.getId()).cookie(admin)).andExpect(status().isOk());
        mvc.perform(post("/api/admin/signup-requests/{id}/approve", r.getId()).cookie(admin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_REVIEWED"));
        reject(r.getId(), "다시 반려해 봅니다").andExpect(status().isConflict());
    }

    @Test
    void 상세에서_같은_사업자번호의_이전_요청을_본다() throws Exception {
        var old = createSignup("2148700124", "소금빵연구소");
        reject(old.getId(), "서류가 흐립니다").andExpect(status().isOk());
        var again = createSignup("2148700124", "소금빵연구소");

        mvc.perform(get("/api/admin/signup-requests/{id}", again.getId()).cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.history.length()").value(1))
                .andExpect(jsonPath("$.history[0].status").value("REJECTED"));
    }

    @Test
    void 없는_요청은_404() throws Exception {
        mvc.perform(get("/api/admin/signup-requests/{id}", 999999).cookie(admin)).andExpect(status().isNotFound());
        mvc.perform(post("/api/admin/signup-requests/{id}/approve", 999999).cookie(admin)).andExpect(status().isNotFound());
    }

    @Test
    void 사업자_토큰으로는_승인할_수_없다() throws Exception {
        createOwner("1234567891", "달콤케이크");
        var r = createSignup("2148700124", "소금빵연구소");
        String appToken = appLogin("1234567891").getValue();
        mvc.perform(post("/api/admin/signup-requests/{id}/approve", r.getId())
                        .header("Authorization", "Bearer " + appToken))
                .andExpect(status().isUnauthorized());
    }
}
