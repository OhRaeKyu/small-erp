package com.smallerp.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.smallerp.account.Tenant;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class TenantSettingsTest extends IntegrationTestSupport {

    static final String HOURS = """
            [{"day":"MON","closed":false,"open":"10:00","close":"19:00"},
             {"day":"TUE","closed":false,"open":"10:00","close":"19:00"},
             {"day":"SUN","closed":true}]""";

    Tenant mine;
    Tenant other;
    Cookie token;

    @BeforeEach
    void setUp() throws Exception {
        mine = createOwner("2148700124", "소금빵연구소");
        other = createOwner("1234567891", "다른가게");
        token = appLogin("2148700124");
    }

    @Test
    void 처음_조회하면_월부터_일까지_모두_쉬는_날로_보인다() throws Exception {
        mvc.perform(get("/api/app/tenant").cookie(token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("소금빵연구소"))
                .andExpect(jsonPath("$.businessNo").value("214-87-00124"))
                .andExpect(jsonPath("$.industryName").isNotEmpty())
                .andExpect(jsonPath("$.businessHours.length()").value(7))
                .andExpect(jsonPath("$.businessHours[0].day").value("MON"))
                .andExpect(jsonPath("$.businessHours[6].closed").value(true));
    }

    @Test
    void 사업장_정보를_수정한다() throws Exception {
        mvc.perform(put("/api/app/tenant").cookie(token).contentType(MediaType.APPLICATION_JSON)
                        .content(body("소금빵연구소 본점", HOURS, "40", "2")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("소금빵연구소 본점"))
                .andExpect(jsonPath("$.address").value("서울시 마포구"))
                .andExpect(jsonPath("$.businessHours.length()").value(7))
                .andExpect(jsonPath("$.businessHours[0].open").value("10:00"))
                .andExpect(jsonPath("$.businessHours[2].closed").value(true))
                .andExpect(jsonPath("$.defaultDailyCapacity").value(40))
                .andExpect(jsonPath("$.orderLeadDays").value(2));

        tenants.flush();
        Tenant saved = tenants.findById(mine.getId()).orElseThrow();
        assertThat(saved.getOrderLeadDays()).isEqualTo(2);
        assertThat(tenants.findById(other.getId()).orElseThrow().getName()).isEqualTo("다른가게");
    }

    @Test
    void 요청에_다른_사업장_id를_넣어도_내_사업장만_바뀐다() throws Exception {
        String withTenantId = body("바꿔치기", HOURS, "10", "1").replaceFirst("\\{", "{\"tenantId\":" + other.getId() + ",");
        mvc.perform(put("/api/app/tenant").cookie(token).contentType(MediaType.APPLICATION_JSON).content(withTenantId))
                .andExpect(status().isOk());

        tenants.flush();
        assertThat(tenants.findById(mine.getId()).orElseThrow().getName()).isEqualTo("바꿔치기");
        assertThat(tenants.findById(other.getId()).orElseThrow().getName()).isEqualTo("다른가게");
    }

    @Test
    void 마감이_시작보다_빠르면_요일별_오류를_돌려준다() throws Exception {
        String bad = """
                [{"day":"MON","closed":false,"open":"19:00","close":"10:00"},
                 {"day":"TUE","closed":false,"open":"9시","close":"18:00"}]""";
        mvc.perform(put("/api/app/tenant").cookie(token).contentType(MediaType.APPLICATION_JSON)
                        .content(body("소금빵연구소", bad, "null", "null")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.fieldErrors['businessHours.MON']").value("마감 시간은 시작 시간보다 늦어야 합니다."))
                .andExpect(jsonPath("$.fieldErrors['businessHours.TUE']").value("시간을 00:00 형식으로 입력해 주세요."));
    }

    @Test
    void 상호가_비었거나_숫자가_범위를_벗어나면_거부한다() throws Exception {
        mvc.perform(put("/api/app/tenant").cookie(token).contentType(MediaType.APPLICATION_JSON)
                        .content(body(" ", HOURS, "0", "61")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.defaultDailyCapacity").exists())
                .andExpect(jsonPath("$.fieldErrors.orderLeadDays").exists());
    }

    @Test
    void 로그인하지_않으면_401() throws Exception {
        mvc.perform(get("/api/app/tenant")).andExpect(status().isUnauthorized());
    }

    @Test
    void 관리자_토큰으로는_사업장_정보를_볼_수_없다() throws Exception {
        createAdmin("ops@test.com");
        mvc.perform(get("/api/app/tenant").cookie(new Cookie("APP_TOKEN", adminLogin("ops@test.com").getValue())))
                .andExpect(status().isUnauthorized());
    }

    private static String body(String name, String hours, String capacity, String leadDays) {
        return """
                {"name":"%s","address":"서울시 마포구","phone":"02-123-4567","businessHours":%s,
                 "defaultDailyCapacity":%s,"orderLeadDays":%s}""".formatted(name, hours, capacity, leadDays);
    }
}
