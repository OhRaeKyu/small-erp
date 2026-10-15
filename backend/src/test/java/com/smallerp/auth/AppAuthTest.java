package com.smallerp.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AppAuthTest extends IntegrationTestSupport {

    private org.springframework.test.web.servlet.ResultActions login(String loginId, String password) throws Exception {
        return mvc.perform(post("/api/app/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"loginId\":\"" + loginId + "\",\"password\":\"" + password + "\"}"));
    }

    @Test
    void 대표는_하이픈_있는_사업자번호로도_로그인한다() throws Exception {
        createOwner("1234567891", "달콤케이크");
        login("123-45-67891", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenant.name").value("달콤케이크"))
                .andExpect(jsonPath("$.tenant.businessNo").value("123-45-67891"))
                .andExpect(jsonPath("$.role").value("OWNER"));
    }

    @Test
    void 로그인_쿠키로_내_정보를_조회한다() throws Exception {
        createOwner("1234567891", "달콤케이크");
        Cookie cookie = appLogin("1234567891");
        mvc.perform(get("/api/app/auth/me").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenant.name").value("달콤케이크"));
    }

    @Test
    void 비밀번호가_틀리면_401() throws Exception {
        createOwner("1234567891", "달콤케이크");
        login("1234567891", "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void 승인_대기_중이면_안내한다() throws Exception {
        createSignup("2148700124", "소금빵연구소");
        login("214-87-00124", PASSWORD)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SIGNUP_PENDING"));
        // 비밀번호가 다르면 신청 여부를 드러내지 않는다
        login("214-87-00124", "wrong-password")
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void 반려된_신청이면_안내한다() throws Exception {
        var r = createSignup("2148700124", "소금빵연구소");
        r.markRejected(createAdmin("a@test.com").getId(), "서류 불일치");
        signupRequests.flush();
        login("2148700124", PASSWORD)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SIGNUP_REJECTED"));
        login("2148700124", "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void 정지된_사업장은_로그인할_수_없다() throws Exception {
        createOwner("1234567891", "달콤케이크").suspend();
        tenants.flush();
        login("1234567891", PASSWORD)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("TENANT_SUSPENDED"));
    }

    @Test
    void 관리자_토큰으로는_사용자_API를_쓸_수_없다() throws Exception {
        createAdmin("admin@test.com");
        String adminToken = adminLogin("admin@test.com").getValue();
        mvc.perform(get("/api/app/auth/me").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 만료되거나_망가진_쿠키가_있어도_로그인은_된다() throws Exception {
        createOwner("1234567891", "달콤케이크");
        mvc.perform(post("/api/app/auth/login").cookie(new Cookie("APP_TOKEN", "broken.token.value"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"1234567891\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk());
    }
}
