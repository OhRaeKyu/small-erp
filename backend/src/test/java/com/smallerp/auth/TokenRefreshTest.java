package com.smallerp.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

class TokenRefreshTest extends IntegrationTestSupport {

    private MockHttpServletResponse loginOwner() throws Exception {
        return mvc.perform(post("/api/app/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"1234567891\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse();
    }

    @Test
    void 로그인하면_액세스와_리프레시_쿠키를_함께_준다() throws Exception {
        createOwner("1234567891", "달콤케이크");
        MockHttpServletResponse res = loginOwner();
        Cookie refresh = res.getCookie("APP_REFRESH");
        assertThat(res.getCookie("APP_TOKEN")).isNotNull();
        assertThat(refresh).isNotNull();
        assertThat(refresh.isHttpOnly()).isTrue();
        assertThat(refresh.getPath()).isEqualTo("/api/app/auth");
    }

    @Test
    void 리프레시_쿠키로_새_토큰을_받는다() throws Exception {
        createOwner("1234567891", "달콤케이크");
        Cookie refresh = loginOwner().getCookie("APP_REFRESH");
        MockHttpServletResponse res = mvc.perform(post("/api/app/auth/refresh").cookie(refresh))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenant.name").value("달콤케이크"))
                .andReturn().getResponse();
        mvc.perform(get("/api/app/auth/me").cookie(res.getCookie("APP_TOKEN"))).andExpect(status().isOk());
    }

    @Test
    void 리프레시_토큰은_API_인증에_쓸_수_없다() throws Exception {
        createOwner("1234567891", "달콤케이크");
        String refresh = loginOwner().getCookie("APP_REFRESH").getValue();
        mvc.perform(get("/api/app/auth/me").header("Authorization", "Bearer " + refresh))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 액세스_토큰으로는_갱신할_수_없다() throws Exception {
        createOwner("1234567891", "달콤케이크");
        String access = loginOwner().getCookie("APP_TOKEN").getValue();
        mvc.perform(post("/api/app/auth/refresh").cookie(new Cookie("APP_REFRESH", access)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 사업장이_정지되면_갱신할_수_없다() throws Exception {
        var tenant = createOwner("1234567891", "달콤케이크");
        Cookie refresh = loginOwner().getCookie("APP_REFRESH");
        tenant.suspend();
        tenants.flush();
        mvc.perform(post("/api/app/auth/refresh").cookie(refresh))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("TENANT_SUSPENDED"));
    }

    @Test
    void 관리자_리프레시_토큰은_사용자_갱신에_쓸_수_없다() throws Exception {
        createAdmin("admin@test.com");
        MockHttpServletResponse res = mvc.perform(post("/api/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@test.com\",\"password\":\"" + PASSWORD + "\"}"))
                .andReturn().getResponse();
        String adminRefresh = res.getCookie("ADMIN_REFRESH").getValue();
        mvc.perform(post("/api/admin/auth/refresh").cookie(new Cookie("ADMIN_REFRESH", adminRefresh)))
                .andExpect(status().isOk());
        mvc.perform(post("/api/app/auth/refresh").cookie(new Cookie("APP_REFRESH", adminRefresh)))
                .andExpect(status().isUnauthorized());
    }
}
