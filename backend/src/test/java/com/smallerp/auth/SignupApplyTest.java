package com.smallerp.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class SignupApplyTest extends IntegrationTestSupport {

    private String body(String businessNo) {
        return """
                {"businessNo":"%s","companyName":"소금빵연구소","ownerName":"이소금","industryCode":"BAKERY_DESSERT",
                 "phone":"010-1111-2222","email":"Salt@Bread.test","password":"password1!"}
                """.formatted(businessNo);
    }

    private org.springframework.test.web.servlet.ResultActions apply(String businessNo) throws Exception {
        return mvc.perform(post("/api/public/signup-requests").contentType(MediaType.APPLICATION_JSON)
                .content(body(businessNo)));
    }

    @Test
    void 가입_신청은_대기_상태로_저장된다() throws Exception {
        apply("214-87-00124")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void 검증번호가_틀린_사업자번호는_막는다() throws Exception {
        apply("214-87-00125")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_BUSINESS_NO"));
    }

    @Test
    void 대기_중인_신청이_있으면_다시_신청할_수_없다() throws Exception {
        apply("2148700124").andExpect(status().isCreated());
        apply("214-87-00124")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SIGNUP_ALREADY_PENDING"));
    }

    @Test
    void 이미_가입된_사업자번호는_막는다() throws Exception {
        createOwner("2148700124", "소금빵연구소");
        apply("2148700124")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_REGISTERED"));
    }

    @Test
    void 비밀번호는_영문과_숫자를_모두_포함해야_한다() throws Exception {
        mvc.perform(post("/api/public/signup-requests").contentType(MediaType.APPLICATION_JSON)
                        .content(body("2148700124").replace("password1!", "onlyletters")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").value("비밀번호에 영문과 숫자를 모두 넣어 주세요."));
    }

    @Test
    void 반려된_번호는_다시_신청할_수_있고_이전_신청은_남는다() throws Exception {
        var rejected = createSignup("2148700124", "소금빵연구소");
        rejected.markRejected(createAdmin("a@test.com").getId(), "서류 불일치");
        signupRequests.flush();
        apply("2148700124").andExpect(status().isCreated());
        org.assertj.core.api.Assertions.assertThat(signupRequests.findAll())
                .extracting(r -> r.getStatus().name())
                .containsExactlyInAnyOrder("REJECTED", "PENDING");
    }

    @Test
    void 필수값이_없으면_필드별_오류를_준다() throws Exception {
        mvc.perform(post("/api/public/signup-requests").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.businessNo").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }
}
