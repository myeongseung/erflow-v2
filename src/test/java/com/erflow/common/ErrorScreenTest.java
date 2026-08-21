package com.erflow.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 로그인 없이 보이는 화면들.
 *
 * <p>오류 화면과 비밀번호 찾기는 로그인하지 않은 사람이 보는 화면이다. 권한 설정이
 * 어긋나면 오류 대신 로그인 화면이 뜨는데, 그것은 레거시와 다른 화면이다.
 */
@SpringBootTest(properties = "server.port=0")
@AutoConfigureMockMvc
@ActiveProfiles("local")
class ErrorScreenTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeAll
    static void requireLocalConfig() {
        assumeTrue(
                new ClassPathResource("application-local.yml").exists(),
                "application-local.yml 이 없어 건너뛴다");
    }

    /**
     * 로그인하지 않은 채 화면을 연다.
     *
     * @param path 경로
     * @return 받은 HTML
     */
    private String openAnonymously(String path) throws Exception {
        return mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    @DisplayName("404 화면은 주소로도 열린다")
    void notFoundScreenHasItsOwnAddress() throws Exception {
        // 레거시에서 notFoundError.jsp 는 오류 화면이면서 그냥 열리는 주소이기도 했다.
        assertThat(openAnonymously("/not-found-error"))
                .contains("404")
                .contains("페이지를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("500 화면도 주소로 열린다")
    void internalServerErrorScreenHasItsOwnAddress() throws Exception {
        assertThat(openAnonymously("/internal-server-error"))
                .contains("500")
                .contains("내부 서버 오류입니다.");
    }

    @Test
    @DisplayName("비밀번호 찾기는 로그인 전에 열린다")
    void findPasswordIsOpenBeforeLogin() throws Exception {
        // 로그인 화면의 «Forgot Password?» 가 여기로 온다. 그동안 갈 곳이 없었다.
        assertThat(openAnonymously("/login/find-password"))
                .contains("Forgot your password?")
                .contains("name=\"email\"");
    }

    @Test
    @DisplayName("메일 보내기는 아직 받는 곳이 없다")
    void sendingMailIsNotMigratedYet() throws Exception {
        // 화면만 옮겼다(O-011). 주소를 레거시 그대로 두어 «비어 있다»가 드러난다 —
        // 슬그머니 다른 곳으로 보내면 없는 기능이 있는 것처럼 보인다.
        assertThat(openAnonymously("/login/find-password"))
                .contains("action=\"../mail/SendMail\"");
    }
}
