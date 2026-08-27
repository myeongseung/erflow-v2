package com.erflow.admin.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.erflow.auth.TestUsers;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 문서 양식 화면이 실제 데이터로 도는지 확인한다.
 *
 * <p>양식 내용은 HTML 이라 화면에 <b>그대로</b> 나가야 한다(목록의 모달, 편집기 본문).
 * 글자가 escape 되면 서식이 태그째로 보인다 — 게이트는 그 차이를 못 본다.
 */
@SpringBootTest(properties = "server.port=0")
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AdminTemplateScreenTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminTemplateService templateService;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeAll
    static void requireLocalConfig() {
        assumeTrue(
                new ClassPathResource("application-local.yml").exists(),
                "application-local.yml 이 없어 건너뛴다");
    }

    @Test
    @DisplayName("문서 양식 리스트가 그려진다")
    void listRenders() throws Exception {
        String html = mockMvc.perform(
                        get("/admin/document/form-list").with(user(TestUsers.admin())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("문서 번호").contains("문서 제목").contains("내용 보기");
    }

    @Test
    @DisplayName("줄마다 자기 양식이 열린다 — id 가 겹치지 않는다(D-121)")
    void everyRowOpensItsOwnPreview() throws Exception {
        String html = mockMvc.perform(
                        get("/admin/document/form-list").with(user(TestUsers.admin())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<String> ids = matches(html, "id=\"(template-preview-\\d+)\"");
        List<String> targets = matches(html, "data-bs-target=\"#(template-preview-\\d+)\"");

        assumeTrue(!ids.isEmpty(), "양식이 없어 건너뛴다");

        // 레거시는 셋 다 같은 id(calendar-plus)라 어느 줄을 눌러도 첫 줄이 떴다(D-066).
        // 중복이 하나라도 있으면 그 화면이 다시 그렇게 된다.
        assertThat(ids).as("모달 id 는 겹치지 않는다").doesNotHaveDuplicates();

        // 버튼과 모달이 짝이 맞아야 «내가 누른 줄» 이 열린다.
        assertThat(targets).as("버튼이 가리키는 곳").containsExactlyElementsOf(ids);

        assertThat(html).as("옛 이름이 남아 있지 않다").doesNotContain("calendar-plus");
    }

    private static List<String> matches(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        List<String> found = new ArrayList<>();
        while (matcher.find()) {
            found.add(matcher.group(1));
        }
        return found;
    }

    @Test
    @DisplayName("양식 내용은 HTML 로 나가지만 걸러서 나간다(D-118)")
    @Transactional
    void contentIsRenderedAsSanitisedHtml() throws Exception {
        templateService.create("시험양식",
                "<table><tr><td>기 안 서</td></tr></table><script>alert('probe')</script>");

        String html = mockMvc.perform(
                        get("/admin/document/form-list").with(user(TestUsers.admin())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // escape 되면 &lt;table&gt; 로 나온다. 그러면 서식이 아니라 태그가 보인다.
        // 정화기가 표를 정규화해 tbody 를 채워 넣는다 — 브라우저가 하는 일과 같아서
        // 화면은 달라지지 않는다.
        assertThat(html).contains("<table><tbody><tr><td>기 안 서</td></tr></tbody></table>");
        // 1단계는 이 script 를 그대로 내보냈다(O-010). 이제 사라진다.
        assertThat(html).doesNotContain("alert('probe')").doesNotContain("<script>alert");
    }

    @Test
    @DisplayName("추가 화면은 비어 있고, 수정 화면은 값이 채워진다")
    void formRegisterFillsOnUpdate() throws Exception {
        String blank = mockMvc.perform(
                        get("/admin/document/form-register").with(user(TestUsers.admin())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        // 추가일 때 숨은 id 는 -1 이다. 레거시 그대로다.
        assertThat(blank).contains("value=\"-1\"").contains("value=\"insert\"");

        TemplateRow any = templateService.list().get(0);
        String filled = mockMvc.perform(get("/admin/document/form-register")
                        .param("flag", "update").param("id", String.valueOf(any.id()))
                        .with(user(TestUsers.admin())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(filled).contains("value=\"update\"")
                .contains("value=\"" + any.id() + "\"")
                .contains(any.subject());
    }

    @Test
    @DisplayName("없는 양식을 고치려 하면 잘못된 접근으로 보낸다")
    void updateFormWithoutTargetRedirects() throws Exception {
        mockMvc.perform(get("/admin/document/form-register")
                        .param("flag", "update").param("id", "-99999")
                        .with(user(TestUsers.admin())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-error"));
    }

    @Test
    @DisplayName("등록하고 고치고 지운다")
    @Transactional
    void createUpdateDelete() {
        assertThat(templateService.create("시험양식", "<p>처음</p>")).isTrue();
        int id = jdbc.queryForObject(
                "SELECT id FROM template_tbl WHERE subject = '시험양식'", Integer.class);

        assertThat(templateService.update(id, "고친양식", "<p>나중</p>")).isTrue();
        var row = jdbc.queryForMap("SELECT subject, content FROM template_tbl WHERE id = ?", id);
        assertThat(row.get("subject")).isEqualTo("고친양식");
        assertThat(row.get("content")).isEqualTo("<p>나중</p>");

        assertThat(templateService.delete(List.of(id))).isTrue();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM template_tbl WHERE id = ?", Integer.class, id)).isZero();
    }

    @Test
    @DisplayName("없는 번호는 고치지도 지우지도 못한다")
    @Transactional
    void missingTargetFails() {
        assertThat(templateService.update(-99999, "없는양식", "<p>내용</p>")).isFalse();
        assertThat(templateService.delete(List.of(-99999))).isFalse();
    }

    @Test
    @DisplayName("관리자가 아니면 문서 양식 화면을 볼 수 없다")
    void nonAdminIsBlocked() throws Exception {
        mockMvc.perform(get("/admin/document/form-list")
                        .with(user(TestUsers.noPermission())))
                .andExpect(status().isForbidden());
    }
}
