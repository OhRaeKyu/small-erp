package com.smallerp.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AdminAuthAndSignupListTest extends IntegrationTestSupport {

    @BeforeEach
    void setUp() {
        createAdmin("admin@test.com");
    }

    @Test
    void 관리자_로그인하면_httpOnly_쿠키를_준다() throws Exception {
        Cookie cookie = adminLogin("ADMIN@test.com");
        assertThat(cookie.isHttpOnly()).isTrue();
        mvc.perform(get("/api/admin/auth/me").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@test.com"));
    }

    @Test
    void 비밀번호가_틀리면_401() throws Exception {
        mvc.perform(post("/api/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@test.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void 로그인_없이_신청_목록을_볼_수_없다() throws Exception {
        mvc.perform(get("/api/admin/signup-requests"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void 사업자_토큰으로는_관리자_API를_쓸_수_없다() throws Exception {
        createOwner("1234567891", "달콤케이크");
        Cookie appCookie = appLogin("1234567891");
        String appToken = appCookie.getValue();

        mvc.perform(get("/api/admin/signup-requests").header("Authorization", "Bearer " + appToken))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/signup-requests").cookie(new Cookie("ADMIN_TOKEN", appToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 신청_목록을_상태별로_거르고_검색한다() throws Exception {
        createSignup("2148700124", "소금빵연구소");
        createSignup("1058123457", "할매수제청");
        var rejected = createSignup("2208111115", "나무결 공방");
        rejected.markRejected(admins.findAll().get(0).getId(), "서류 불일치");
        signupRequests.flush();
        Cookie cookie = adminLogin("admin@test.com");

        // 기본 정렬: 오래된 신청부터
        mvc.perform(get("/api/admin/signup-requests").param("status", "PENDING").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].businessNo").value("214-87-00124"))
                .andExpect(jsonPath("$.content[0].industryName").value("베이커리·디저트"));

        mvc.perform(get("/api/admin/signup-requests").param("status", "PENDING").param("sort", "LATEST").cookie(cookie))
                .andExpect(jsonPath("$.content[0].companyName").value("할매수제청"));

        mvc.perform(get("/api/admin/signup-requests").param("q", "214-87").cookie(cookie))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].companyName").value("소금빵연구소"));

        mvc.perform(get("/api/admin/signup-requests").param("q", "공방").cookie(cookie))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("REJECTED"));

        mvc.perform(get("/api/admin/signup-requests").param("q", "%").cookie(cookie))
                .andExpect(jsonPath("$.totalElements").value(0));

        mvc.perform(get("/api/admin/signup-requests/counts").cookie(cookie))
                .andExpect(jsonPath("$.PENDING").value(2))
                .andExpect(jsonPath("$.APPROVED").value(0))
                .andExpect(jsonPath("$.REJECTED").value(1));
    }

    @Test
    void 잘못된_상태값은_400() throws Exception {
        Cookie cookie = adminLogin("admin@test.com");
        mvc.perform(get("/api/admin/signup-requests").param("status", "WHATEVER").cookie(cookie))
                .andExpect(status().isBadRequest());
    }
}
