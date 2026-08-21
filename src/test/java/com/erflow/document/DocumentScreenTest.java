package com.erflow.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.erflow.auth.TestUsers;
import java.util.List;
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
 * 문서 화면이 실제 데이터로 도는지 확인한다.
 *
 * <p>이 도메인의 핵심은 <b>결재에 걸린 문서를 건드리지 않는 것</b>이다. 고치면 새로
 * 만들고, 지우려 하면 지우지 않는다 — 결재선이 본 내용이 달라지면 안 되기 때문이다.
 * 게이트는 그 차이를 못 본다.
 */
@SpringBootTest(properties = "server.port=0")
@AutoConfigureMockMvc
@ActiveProfiles("local")
class DocumentScreenTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeAll
    static void requireLocalConfig() {
        assumeTrue(
                new ClassPathResource("application-local.yml").exists(),
                "application-local.yml 이 없어 건너뛴다");
    }

    @Test
    @DisplayName("문서 리스트가 그려진다")
    void listRenders() throws Exception {
        String html = mockMvc.perform(get("/document/list").with(user(TestUsers.admin())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("사용자 &gt; 문서 관리 &gt; 문서 리스트")
                .contains("문서번호").contains("양식명").contains("결재 상태");
    }

    @Test
    @DisplayName("내 문서만 보인다")
    void listShowsOnlyMine() {
        String owner = jdbc.queryForObject(
                "SELECT user_tbl_id FROM document_tbl LIMIT 1", String.class);
        assumeTrue(owner != null, "문서가 없어 건너뛴다");

        var mine = documentService.list(DocumentSearch.none(), owner, 1);
        var others = documentService.list(DocumentSearch.none(), "no-such-user", 1);

        assertThat(mine.rows()).isNotEmpty();
        assertThat(others.rows()).isEmpty();
        for (DocumentListRow row : mine.rows()) {
            assertThat(jdbc.queryForObject(
                    "SELECT user_tbl_id FROM document_tbl WHERE id = ?", String.class, row.id()))
                    .isEqualTo(owner);
        }
    }

    @Test
    @DisplayName("양식 없는 문서와 고치지 않은 문서에 안내 글자가 뜬다")
    void labelsFillTheBlanks() {
        var row = new DocumentListRow(1, "제목", null, "2023-01-01", null, 0, 0);

        assertThat(row.templateLabel()).isEqualTo("(빈 문서)");
        assertThat(row.updatedLabel()).isEqualTo("(수정하지 않음)");
        assertThat(new DocumentListRow(1, "제목", "기안서", "2023-01-01", "2023-01-02", 0, 0)
                .templateLabel()).isEqualTo("기안서");
    }

    @Test
    @DisplayName("작성 화면이 양식 목록과 함께 그려진다")
    void registerFormRenders() throws Exception {
        String html = mockMvc.perform(get("/document/register").with(user(TestUsers.admin())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("사용자 &gt; 문서 관리 &gt; 기안 작성")
                .contains("빈 문서")
                // 새 문서일 때 숨은 id 는 -1 이다.
                .contains("value=\"-1\"").contains("value=\"insert\"");
    }

    @Test
    @DisplayName("남의 문서는 고치러 들어갈 수 없다")
    void cannotEditSomeoneElsesDocument() throws Exception {
        var row = jdbc.queryForMap(
                "SELECT id, user_tbl_id FROM document_tbl WHERE user_tbl_id != 'admin' LIMIT 1");
        assumeTrue(!row.isEmpty(), "남의 문서가 없어 건너뛴다");

        mockMvc.perform(get("/document/register")
                        .param("flag", "update")
                        .param("docId", String.valueOf(row.get("id")))
                        .with(user(TestUsers.admin())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-error"));
    }

    @Test
    @DisplayName("양식을 고르면 그 내용이 편집기에 부어진다")
    void choosingTemplateFillsTheEditor() {
        var templates = documentService.templates();
        assumeTrue(!templates.isEmpty(), "문서 양식이 없어 건너뛴다");

        assertThat(documentService.templateContent(templates.get(0).id()))
                .isEqualTo(templates.get(0).content());
        // 빈 문서(0)와 없는 양식은 빈 내용이다.
        assertThat(documentService.templateContent(DocumentService.BLANK_TEMPLATE)).isEmpty();
        assertThat(documentService.templateContent(-999)).isEmpty();
    }

    @Test
    @DisplayName("양식을 골라도 쓰던 글은 남는다 — D-076 을 2단계에서 고쳤다(D-110)")
    @Transactional
    void templateNoLongerWipesSavedContent() throws Exception {
        var template = documentService.templates().stream()
                .filter(t -> t.content() != null && !t.content().isBlank())
                .findFirst().orElse(null);
        assumeTrue(template != null, "내용 있는 양식이 없어 건너뛴다");

        documentService.create("admin", 0, "덮기 시험", "<p>쓰던 글</p>");
        long docId = jdbc.queryForObject(
                "SELECT MAX(id) FROM document_tbl WHERE user_tbl_id = 'admin'", Long.class);

        // 레거시는 양식을 고르면 무조건 그 내용으로 덮어 쓰던 글이 사라졌다(D-076).
        var kept = mockMvc.perform(get("/document/register")
                        .param("flag", "update")
                        .param("docId", String.valueOf(docId))
                        .param("template", String.valueOf(template.id()))
                        .with(user(TestUsers.admin())))
                .andExpect(status().isOk())
                .andReturn().getModelAndView().getModel();
        assertThat(kept.get("content")).isEqualTo("<p>쓰던 글</p>");

        // 내용이 비어 있으면 양식이 채운다 — 양식을 고르는 뜻은 남는다.
        documentService.create("admin", 0, "빈 문서 시험", "");
        long blankId = jdbc.queryForObject(
                "SELECT MAX(id) FROM document_tbl WHERE user_tbl_id = 'admin'", Long.class);
        var filled = mockMvc.perform(get("/document/register")
                        .param("flag", "update")
                        .param("docId", String.valueOf(blankId))
                        .param("template", String.valueOf(template.id()))
                        .with(user(TestUsers.admin())))
                .andExpect(status().isOk())
                .andReturn().getModelAndView().getModel();
        assertThat(filled.get("content")).isEqualTo(template.content());
    }

    @Test
    @DisplayName("등록하면 내 문서로 저장된다")
    @Transactional
    void createStoresMine() {
        assertThat(documentService.create("admin", 0, "시험문서", "<p>본문</p>")).isTrue();

        var row = jdbc.queryForMap(
                "SELECT user_tbl_id, subject, content, doc_status, proposal_status, updated_at "
                        + "FROM document_tbl WHERE subject = '시험문서'");
        assertThat(row.get("user_tbl_id")).isEqualTo("admin");
        assertThat(row.get("content")).isEqualTo("<p>본문</p>");
        assertThat(((Number) row.get("doc_status")).intValue()).isZero();
        // 새 문서는 아직 고치지 않은 상태다.
        assertThat(row.get("updated_at")).isNull();
    }

    @Test
    @DisplayName("결재에 걸리지 않은 문서는 그 자리에서 고쳐진다")
    @Transactional
    void updateEditsInPlace() {
        documentService.create("admin", 0, "시험문서", "<p>처음</p>");
        long id = newDocumentId();

        var done = documentService.update(id, "admin", 0, "고친제목", "<p>나중</p>");

        assertThat(done).isEqualTo(DocumentService.UpdateResult.UPDATED);
        var row = jdbc.queryForMap(
                "SELECT subject, content, updated_at FROM document_tbl WHERE id = ?", id);
        assertThat(row.get("subject")).isEqualTo("고친제목");
        assertThat(row.get("updated_at")).isNotNull();
    }

    @Test
    @DisplayName("결재에 걸린 문서를 고치면 새 문서가 된다 — 원본은 그대로다")
    @Transactional
    void updateOnProposedDocumentCreatesNewOne() {
        documentService.create("admin", 0, "시험문서", "<p>처음</p>");
        long id = newDocumentId();
        // 그 문서로 결재를 하나 올려 둔다.
        int routeId = jdbc.queryForObject(
                "SELECT id FROM proposal_route_tbl LIMIT 1", Integer.class);
        jdbc.update("INSERT INTO proposal_tbl (document_tbl_id, user_tbl_id, "
                + "proposal_route_tbl_id, step, result, received_at) "
                + "VALUES (?, 'admin', ?, 0, 3, NOW())", id, routeId);

        var done = documentService.update(id, "admin", 0, "고친제목", "<p>나중</p>");

        assertThat(done).isEqualTo(DocumentService.UpdateResult.RENEWED);
        // 원본은 손대지 않는다.
        assertThat(jdbc.queryForObject(
                "SELECT subject FROM document_tbl WHERE id = ?", String.class, id))
                .isEqualTo("시험문서");
        // 새 문서가 하나 생긴다.
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM document_tbl WHERE subject = '고친제목'", Integer.class))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("결재에 걸린 문서는 지워지지 않는다")
    @Transactional
    void proposedDocumentIsNotDeleted() {
        documentService.create("admin", 0, "시험문서", "<p>본문</p>");
        long id = newDocumentId();
        int routeId = jdbc.queryForObject(
                "SELECT id FROM proposal_route_tbl LIMIT 1", Integer.class);
        jdbc.update("INSERT INTO proposal_tbl (document_tbl_id, user_tbl_id, "
                + "proposal_route_tbl_id, step, result, received_at) "
                + "VALUES (?, 'admin', ?, 0, 3, NOW())", id, routeId);

        assertThat(documentService.delete(List.of(id))).isFalse();

        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM document_tbl WHERE id = ?", Integer.class, id))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("결재에 걸리지 않은 문서는 지워진다")
    @Transactional
    void plainDocumentIsDeleted() {
        documentService.create("admin", 0, "시험문서", "<p>본문</p>");
        long id = newDocumentId();

        assertThat(documentService.delete(List.of(id))).isTrue();

        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM document_tbl WHERE id = ?", Integer.class, id)).isZero();
    }

    @Test
    @DisplayName("검색은 칸마다 조건이 다르다 — 문서ID 만 정확히 같은 값")
    void searchDiffersByField() {
        String owner = jdbc.queryForObject(
                "SELECT user_tbl_id FROM document_tbl LIMIT 1", String.class);
        assumeTrue(owner != null, "문서가 없어 건너뛴다");
        var any = documentService.list(DocumentSearch.none(), owner, 1).rows().get(0);

        assertThat(documentService.list(
                new DocumentSearch("id", String.valueOf(any.id())), owner, 1).rows())
                .hasSize(1);
        assertThat(documentService.list(
                new DocumentSearch("subject", any.subject()), owner, 1).rows()).isNotEmpty();
        // 숫자가 아니면 조건이 붙지 않는다(레거시는 그 자리에서 죽는다).
        assertThat(documentService.list(new DocumentSearch("id", "숫자아님"), owner, 1).rows())
                .hasSameSizeAs(documentService.list(DocumentSearch.none(), owner, 1).rows());
    }

    @Test
    @DisplayName("권한이 없으면 목록을 볼 수 없다")
    void withoutPermissionBlocked() throws Exception {
        mockMvc.perform(get("/document/list").with(user(TestUsers.noPermission())))
                .andExpect(status().isForbidden());
    }

    private long newDocumentId() {
        return jdbc.queryForObject(
                "SELECT id FROM document_tbl WHERE subject = '시험문서' ORDER BY id DESC LIMIT 1",
                Long.class);
    }
}
