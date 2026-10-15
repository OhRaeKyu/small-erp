package com.smallerp.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

/** 회원가입 화면용 API: 등록증 첨부, 상태 조회, 업종 목록 */
class SignupFormTest extends IntegrationTestSupport {

    static final byte[] PDF = "%PDF-1.4 test certificate".getBytes();

    @Autowired
    ObjectMapper om;

    private MockMultipartFile requestPart(String businessNo) throws Exception {
        Map<String, String> body = Map.of("businessNo", businessNo, "companyName", "소금빵연구소", "ownerName", "이소금",
                "industryCode", "BAKERY_DESSERT", "phone", "010-1111-2222", "email", "salt@bread.test",
                "password", "password1!");
        return new MockMultipartFile("request", "", MediaType.APPLICATION_JSON_VALUE, om.writeValueAsBytes(body));
    }

    private org.springframework.test.web.servlet.ResultActions lookupStatus(String businessNo, String password) throws Exception {
        return mvc.perform(post("/api/public/signup-requests/status").contentType(MediaType.APPLICATION_JSON)
                .content("{\"businessNo\":\"" + businessNo + "\",\"password\":\"" + password + "\"}"));
    }

    @Test
    void 등록증을_첨부해_신청하면_관리자만_볼_수_있다() throws Exception {
        String body = mvc.perform(multipart("/api/public/signup-requests")
                        .file(requestPart("214-87-00124"))
                        .file(new MockMultipartFile("certificate", "cert.pdf", "application/pdf", PDF)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = om.readTree(body).get("id").asLong();

        createAdmin("admin@test.com");
        Cookie admin = adminLogin("admin@test.com");
        mvc.perform(get("/api/admin/signup-requests/{id}", id).cookie(admin))
                .andExpect(jsonPath("$.request.hasCertificate").value(true));
        mvc.perform(get("/api/admin/signup-requests/{id}/certificate", id).cookie(admin))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().bytes(PDF));
        mvc.perform(get("/api/admin/signup-requests/{id}/certificate", id))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 등록증_없이도_multipart로_신청할_수_있다() throws Exception {
        mvc.perform(multipart("/api/public/signup-requests").file(requestPart("2148700124")))
                .andExpect(status().isCreated());
    }

    @Test
    void 허용하지_않는_파일은_거부한다() throws Exception {
        mvc.perform(multipart("/api/public/signup-requests")
                        .file(requestPart("2148700124"))
                        .file(new MockMultipartFile("certificate", "evil.pdf", "application/pdf", "<html>".getBytes())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FILE_TYPE"));
    }

    @Test
    void 용량이_5MB를_넘는_파일은_거부한다() throws Exception {
        byte[] big = new byte[5 * 1024 * 1024 + 1];
        System.arraycopy(PDF, 0, big, 0, PDF.length);
        mvc.perform(multipart("/api/public/signup-requests")
                        .file(requestPart("2148700124"))
                        .file(new MockMultipartFile("certificate", "big.pdf", "application/pdf", big)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FILE_TOO_LARGE"));
    }

    @Test
    void 사업자번호와_비밀번호로_상태를_조회한다() throws Exception {
        createSignup("2148700124", "소금빵연구소");
        lookupStatus("214-87-00124", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.companyName").value("소금빵연구소"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void 틀린_비밀번호로는_상태를_볼_수_없다() throws Exception {
        createSignup("2148700124", "소금빵연구소");
        lookupStatus("2148700124", "wrong-password")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SIGNUP_NOT_FOUND"));
        // 없는 번호와 같은 응답이라 신청 여부가 드러나지 않는다
        lookupStatus("1058123457", PASSWORD)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SIGNUP_NOT_FOUND"));
    }

    @Test
    void 반려되면_사유를_보여주고_다시_신청하면_최신_요청이_보인다() throws Exception {
        var r = createSignup("2148700124", "소금빵연구소");
        r.markRejected(createAdmin("a@test.com").getId(), "서류가 흐립니다");
        signupRequests.flush();
        lookupStatus("2148700124", PASSWORD)
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectReason").value("서류가 흐립니다"));

        createSignup("2148700124", "소금빵연구소");
        lookupStatus("2148700124", PASSWORD).andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void 업종_목록은_기타가_맨_뒤에_온다() throws Exception {
        mvc.perform(get("/api/public/industry-packs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("BAKERY_DESSERT"))
                .andExpect(jsonPath("$[3].code").value("GENERAL"));
    }
}
