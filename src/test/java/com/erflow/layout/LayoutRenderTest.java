package com.erflow.layout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.erflow.auth.TestUsers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 레이아웃 fragment 가 레거시와 같은 것을 그리는지 확인한다.
 *
 * <p>렌더링 결과를 {@code build/rendered/} 에 남긴다. 눈으로 볼 수도 있고 정합성
 * 게이트에 넣을 수도 있다.
 *
 * <p>메뉴 항목의 누락·발명·순서는 {@code check_menu_parity} 가 데이터 층에서 이미
 * 검증한다. 여기서는 그 데이터가 <b>마크업으로 제대로 나오는지</b>를 본다 —
 * 아이콘이나 구분선처럼 글자가 없어 조용히 사라지는 것들이 특히 그렇다.
 */
@SpringBootTest(properties = "server.port=0")
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Import(LayoutRenderTest.PreviewConfig.class)
class LayoutRenderTest {

    private static final Path OUT = Path.of("build", "rendered");

    @Autowired
    private MockMvc mockMvc;

    @BeforeAll
    static void requireLocalConfig() {
        assumeTrue(
                new ClassPathResource("application-local.yml").exists(),
                "application-local.yml 이 없어 건너뛴다");
    }

    private String render(String path, String fileName) throws Exception {
        return render(path, fileName, TestUsers.admin());
    }

    private String render(String path, String fileName,
            com.erflow.auth.ErflowUserDetails as) throws Exception {
        String html = mockMvc.perform(get(path).with(user(as)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Files.createDirectories(OUT);
        Files.writeString(OUT.resolve(fileName), html);
        return html;
    }

    @Test
    @DisplayName("사이드 메뉴가 레거시와 같은 항목을 같은 순서로 그린다")
    void sideMenuMatchesLegacy() throws Exception {
        String html = render("/__preview/side", "side-menu.html");

        // 레거시 indexSide.jsp 의 그룹 6개 + 최상위 링크 1개(게시판)
        List<String> groups =
                List.of("문서 관리", "전자결재", "생산관리", "구매", "영업", "근태 관리", "게시판");
        int cursor = -1;
        for (String group : groups) {
            int found = html.indexOf(group, cursor + 1);
            assertThat(found).as("«%s» 이 순서대로 나와야 한다", group).isGreaterThan(cursor);
            cursor = found;
        }

        // 이 class 이름들에 사이드바 모양이 걸려 있다. 전에는 aside.css 였고
        // 지금은 app.css 다(D-124) — 이름을 바꾸면 사이드바가 무너진다.
        assertThat(html).contains("class=\"sidebar-mainmenu\"", "class=\"nav-flyout\"",
                "class=\"menu-title\"", "class=\"sidebar-ul\"");

        // 구매/영업의 협력업체 관리는 라벨이 같고 링크가 다르다
        assertThat(html).contains("/company/list?flag=1", "/company/list?flag=0");

        // 레거시에 없던 항목이 끼어들지 않았는지
        assertThat(html).doesNotContain("관리자", "설정", "쪽지");
    }

    @Test
    @DisplayName("헤더가 아이콘과 구분선까지 그린다")
    void headerRendersIconsAndSeparator() throws Exception {
        String html = render("/__preview/header", "header.html");

        assertThat(html).contains("쪽지", "프로필", "로그아웃");

        // 아이콘은 글자가 없어 조용히 사라진다. 데이터로 옮긴 이유가 이것이다.
        assertThat(html).contains("fa-regular fa-envelope fa-lg", "fa-solid fa-user-tie fa-lg");

        // 로그아웃 앞의 구분선
        assertThat(html).contains("<hr");

        // 관리자에게는 '설정'이 보인다 (visibility=ADMIN)
        assertThat(html).contains("fa-solid fa-gear", "설정");

        // 헤더의 고정 요소.
        //
        // 「section2」(헤더 아래 파란 띠)는 재설계에서 없앴다(D-119). 관리자 화면과
        // 헤더를 하나로 합치면서 두 벌이던 상단 바가 하나가 됐고, 그 띠는 둘 중
        // 한쪽에만 있던 장식이었다.
        assertThat(html).contains("navbar-brand", "/images/common/logo.png",
                "fa-regular fa-bell fa-lg");
        assertThat(html).as("합친 상단 바").contains("class=\"app-header\"");
    }

    @Test
    @DisplayName("관리자가 아니면 '설정'이 보이지 않는다")
    void adminOnlyItemIsHiddenFromOthers() throws Exception {
        // 레거시는 <% if (adminTester.isAdmin(session)) { %> 로 감쌌다.
        // 그 조건이 스크립틀릿 안에 있어 추출되지 않으므로 seed 에 손으로 고정했고,
        // 실제로 지켜지는지는 여기서 본다.
        String html = render("/__preview/header", "header-nonadmin.html",
                TestUsers.noPermission());

        assertThat(html).doesNotContain("fa-solid fa-gear");
        assertThat(html).doesNotContain(">설정<");
        assertThat(html).as("나머지 항목은 그대로").contains("쪽지", "프로필", "로그아웃");
    }

    /** 테스트 전용 미리보기 화면. 운영 코드에는 포함되지 않는다. */
    @TestConfiguration
    static class PreviewConfig {

        /**
         * @return 미리보기 컨트롤러
         */
        @Bean
        PreviewController previewController() {
            return new PreviewController();
        }
    }

    /** fragment 만 떼어 렌더링하는 테스트 전용 컨트롤러. */
    @Controller
    static class PreviewController {

        /**
         * @return 사이드 메뉴 미리보기 템플릿
         */
        @GetMapping("/__preview/side")
        String side() {
            return "preview-side";
        }

        /**
         * @return 헤더 미리보기 템플릿
         */
        @GetMapping("/__preview/header")
        String header() {
            return "preview-header";
        }
    }
}
