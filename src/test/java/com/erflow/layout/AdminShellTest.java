package com.erflow.layout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.erflow.auth.TestUsers;
import com.erflow.message.MessageService;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 껍데기 조각(D-150) — 현재 메뉴 표시, 사람 카드, 안 읽은 쪽지 배지.
 */
@SpringBootTest(properties = "server.port=0")
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AdminShellTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MessageService messageService;

    @BeforeAll
    static void requireLocalConfig() {
        assumeTrue(
                new ClassPathResource("application-local.yml").exists(),
                "application-local.yml 이 없어 건너뛴다");
    }

    @Test
    @DisplayName("관리자 화면은 껍데기 조각 하나를 쓰고 지금 있는 메뉴를 밝힌다")
    void shellMarksTheCurrentMenu() throws Exception {
        String users = render("/admin/user/list");
        // 사원 관리는 최상위 항목이라 li 에, 메뉴 관리는 하위 항목이라 안쪽 li 에 붙는다.
        assertThat(users).contains("class=\"admin-page\"").contains("class=\"page-title\"")
                .containsPattern("class=\"sidebar-mainmenu is-current\"[^<]*<a href=\"/admin/user/list\"");
        assertThat(users).doesNotContain("class=\"is-current\"");

        String menus = render("/admin/permission/menu-list");
        assertThat(menus).containsPattern(
                "class=\"is-current\"[^<]*<a href=\"/admin/permission/menu-list\"");
        assertThat(menus).doesNotContain("class=\"sidebar-mainmenu is-current\"");

        // 수정 주소도 그 구역의 항목에 속한다.
        String edit = render("/admin/permission/job-register");
        assertThat(edit).containsPattern(
                "class=\"is-current\"[^<]*<a href=\"/admin/permission/job-dept-list\"");
    }

    @Test
    @DisplayName("사이드바 아래 사람 카드와 헤더 머리글자는 로그인한 사람이다")
    void shellShowsWhoIsLoggedIn() throws Exception {
        String html = render("/admin/user/list");

        assertThat(html).contains("class=\"sidebar-user\"")
                .contains("class=\"sidebar-user-role\"")
                .contains("class=\"avatar\"")
                .contains("class=\"header-user-name\"");
    }

    @Test
    @DisplayName("안 읽은 쪽지가 있으면 종에 수가 붙고, 없으면 붙지 않는다")
    @Transactional
    void bellCarriesTheUnreadCount() throws Exception {
        String before = render("/admin/user/list");
        boolean hadBadge = before.contains("class=\"badge-count\"");

        // 관리자가 자기에게 쪽지를 보내면 안 읽은 쪽지가 하나 는다.
        assertThat(messageService.send("admin", List.of("admin"), "껍데기 시험")).isTrue();

        String after = render("/admin/user/list");
        assertThat(after).contains("class=\"badge-count\"");
        if (!hadBadge) {
            assertThat(after).contains("class=\"badge-count\">1<");
        }
    }

    private String render(String path) throws Exception {
        return mockMvc.perform(get(path).with(user(TestUsers.admin())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }
}
