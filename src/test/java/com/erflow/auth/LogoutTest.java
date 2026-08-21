package com.erflow.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 로그아웃이 링크(GET)로 동작하는지 확인한다.
 *
 * <p>레거시 헤더 메뉴의 «로그아웃» 은 링크다. CSRF 를 켜면 프레임워크 기본 로그아웃이
 * POST 만 받아 그 링크가 404 가 된다 — 실제로 그랬다(D-055). 화면을 눌러 보지 않으면
 * 드러나지 않는 자리라 시험으로 박아 둔다.
 */
@SpringBootTest(properties = "server.port=0")
@AutoConfigureMockMvc
@ActiveProfiles("local")
class LogoutTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("로그아웃 링크(GET)가 세션을 끝내고 로그인 화면으로 보낸다")
    void logoutLinkEndsSession() throws Exception {
        HttpSession session = mockMvc.perform(
                        get("/login/logout-proc").with(user(TestUsers.admin())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andReturn().getRequest().getSession(false);

        assertThat(session == null || session.getAttribute("SPRING_SECURITY_CONTEXT") == null)
                .isTrue();
    }
}
