package com.erflow.proposal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.erflow.auth.TestUsers;
import com.erflow.common.SafeHtml;
import java.util.List;
import java.util.Map;
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
 * 결재 리스트·등록·문서 상세가 실제 데이터로 도는지 확인한다.
 *
 * <p>상태 필터(결재진행중/승인/반려)와 인박스 로직(내가 결재선에 든 진행중 + 내가 기안한
 * 선택 상태), 그리고 승인/반려가 결재선을 어떻게 밟아 나가는지를 본다.
 *
 * <p>승인·반려는 상태를 바꾸는 시험이라 모두 {@code @Transactional} 로 되돌린다. 결재선을
 * 처음부터 밟으려면 그 문서의 결재를 지우고 다시 만들어야 하는데, 롤백이 없으면 실행할
 * 때마다 데이터가 달라져 다음 시험이 무엇을 보는지 알 수 없게 된다.
 */
@SpringBootTest(properties = "server.port=0")
@AutoConfigureMockMvc
@ActiveProfiles("local")
class ProposalScreenTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProposalService proposalService;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeAll
    static void requireLocalConfig() {
        assumeTrue(
                new ClassPathResource("application-local.yml").exists(),
                "application-local.yml 이 없어 건너뛴다");
    }

    @Test
    @DisplayName("결재 리스트가 그려진다")
    void listRenders() throws Exception {
        String html = mockMvc.perform(get("/proposal/list").with(user(TestUsers.admin())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("결재 리스트").contains("문서 제목").contains("결재 진행 중")
                .contains("class=\"filter-tabs\"").contains("결재 올리기")
                // 결재 올리기는 목록 위의 대화상자다 — 문서 표와 결재라인 표(D-161)
                .contains("id=\"proposal-register\"").contains("id=\"pick-document-rows\"")
                .contains("id=\"pick-route-rows\"").doesNotContain("data-open-on-load");
    }

    @Test
    @DisplayName("탭은 상태대로 거른다 — 진행 중 결재가 승인·반려 탭에 섞이지 않는다(D-162)")
    @Transactional
    void tabsFilterByState() {
        Chain chain = freshChain();
        String me = chain.route()[0];
        long first = proposalService.create(me, chain.documentId(), chain.routeId());

        // 올린 직후 — 진행 중 탭에만 있다. 레거시는 세 탭 모두에 나왔다.
        assertThat(idsOf(me, ProposalService.IN_PROGRESS)).contains(first);
        assertThat(idsOf(me, ProposalService.APPROVED)).doesNotContain(first);
        assertThat(idsOf(me, ProposalService.REJECTED)).doesNotContain(first);

        // 결재선 끝까지 승인하면 — 내가 올린 결재이므로 승인 탭에 생기고 진행 중에서는 사라진다.
        long id = first;
        for (int step = 0; step < chain.route().length; ++step) {
            proposalService.decide(id, chain.route()[step], "confirm", "확인");
            id = jdbc.queryForObject(
                    "SELECT MAX(id) FROM proposal_tbl WHERE document_tbl_id = ?",
                    Long.class, chain.documentId());
        }
        assertThat(idsOf(me, ProposalService.APPROVED)).contains(first);
        assertThat(idsOf(me, ProposalService.IN_PROGRESS)).doesNotContain(first, id);
        assertThat(idsOf(me, ProposalService.REJECTED)).doesNotContain(first);
    }

    @Test
    @DisplayName("옛 결재 올리기 주소는 결재 리스트를 대화상자가 열린 채로 그린다(D-161)")
    void registerAddressOpensDialog() throws Exception {
        String html = mockMvc.perform(get("/proposal/register").with(user(TestUsers.admin())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("결재 리스트").contains("id=\"proposal-register\"")
                .contains("data-open-on-load").doesNotContain("window.open");
    }

    @Test
    @DisplayName("문서 리스트에서 문서를 달고 오면 그 문서가 미리 골라져 있다(D-161)")
    void registerAddressPicksDocument() throws Exception {
        List<Long> ids = jdbc.queryForList(
                "SELECT d.id FROM document_tbl d WHERE d.user_tbl_id = 'admin' "
                        + "AND NOT EXISTS (SELECT 1 FROM proposal_tbl p "
                        + "WHERE p.document_tbl_id = d.id) LIMIT 1", Long.class);
        assumeTrue(!ids.isEmpty(), "결재에 올릴 수 있는 admin 문서가 없어 건너뛴다");

        String html = mockMvc.perform(get("/proposal/register")
                        .param("documentId", String.valueOf(ids.get(0)))
                        .with(user(TestUsers.admin())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Thymeleaf 는 checked 를 속성 맨 뒤에 붙인다 — 같은 input 안에 있는지만 본다
        assertThat(html).contains("data-open-on-load")
                .containsPattern("id=\"pick-document-" + ids.get(0) + "\"[^>]*checked=\"checked\"");
    }

    @Test
    @DisplayName("등록하면 진행중(result 3, step 0)으로 결재가 생긴다")
    @Transactional
    void createStartsInProgress() {
        // FK 안전한 문서·결재라인을 기존 결재 한 건에서 가져온다. 롤백된다.
        var refs = jdbc.queryForList(
                "SELECT document_tbl_id, proposal_route_tbl_id FROM proposal_tbl LIMIT 1");
        assumeTrue(!refs.isEmpty(), "결재 데이터가 없어 건너뛴다");
        long documentId = ((Number) refs.get(0).get("document_tbl_id")).longValue();
        int routeId = ((Number) refs.get(0).get("proposal_route_tbl_id")).intValue();

        long newId = proposalService.create("admin", documentId, routeId);

        assertThat(newId).isGreaterThan(0);
        var row = jdbc.queryForMap(
                "SELECT step, result FROM proposal_tbl WHERE id = ?", newId);
        assertThat(((Number) row.get("step")).intValue()).isZero();
        assertThat(((Number) row.get("result")).intValue()).isEqualTo(3);
    }

    @Test
    @DisplayName("결재선에 들어 있어도 내 차례가 아니면 목록에 없다")
    @Transactional
    void listShowsOnlyMyTurn() {
        Chain chain = freshChain();
        long first = proposalService.create(chain.route()[0], chain.documentId(), chain.routeId());

        // 아직 첫 차례다. 기안자에게는 보이고, 결재선 둘째에게는 보이지 않는다.
        assertThat(idsOf(chain.route()[0])).contains(first);
        assertThat(idsOf(chain.route()[1])).doesNotContain(first);

        proposalService.decide(first, chain.route()[0], "confirm", "확인");

        // 승인하면 자리가 넘어간다 — 첫 사람의 목록에서 사라지고 둘째에게 생긴다.
        long next = jdbc.queryForObject(
                "SELECT MAX(id) FROM proposal_tbl WHERE document_tbl_id = ?",
                Long.class, chain.documentId());
        assertThat(idsOf(chain.route()[0])).doesNotContain(first, next);
        assertThat(idsOf(chain.route()[1])).contains(next);
    }

    /** 그 사람의 결재진행중 인박스에 뜨는 결재번호. */
    private List<Long> idsOf(String userId) {
        return idsOf(userId, ProposalService.IN_PROGRESS);
    }

    /** 그 사람의 그 탭(진행 중 · 승인 · 반려)에 뜨는 결재번호. */
    private List<Long> idsOf(String userId, int result) {
        return proposalService.list(userId, result, 1).rows()
                .stream().map(ProposalRow::id).toList();
    }

    @Test
    @DisplayName("문서 상세가 그려진다")
    void documentRenders() throws Exception {
        Long id = jdbc.queryForObject("SELECT MAX(id) FROM proposal_tbl", Long.class);
        assumeTrue(id != null, "결재 데이터가 없어 건너뛴다");

        String html = mockMvc.perform(get("/proposal/document")
                        .param("proposalId", String.valueOf(id))
                        .with(user(TestUsers.admin())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("결재 문서 보기").contains("담당").contains("승인")
                .contains("결재 의견").contains("class=\"approval-paper\"");
    }

    @Test
    @DisplayName("실제 문서를 걸러도 표와 서식이 남는다 — 정책이 조여도 여기서 깨진다(D-118)")
    void sanitisingRealDocumentsKeepsStyling() {
        List<String> contents = jdbc.queryForList(
                "SELECT content FROM document_tbl WHERE content LIKE '%<table%' LIMIT 5",
                String.class);
        assumeTrue(!contents.isEmpty(), "표가 든 문서가 없어 건너뛴다");

        for (String raw : contents) {
            String safe = SafeHtml.clean(raw);

            // 결재 문서의 서식은 전부 인라인 style 이다. 이 셋 중 하나라도 사라지면
            // 문서가 테두리 없는 맨 표가 된다.
            assertThat(safe).contains("<table").contains("<td").contains("style=");
            assertThat(safe).doesNotContain("<script").doesNotContain("javascript:");
        }
    }

    @Test
    @DisplayName("결재번호가 없으면 잘못된 접근으로 보낸다")
    void documentWithoutIdRedirects() throws Exception {
        mockMvc.perform(get("/proposal/document").with(user(TestUsers.admin())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-error"));
    }

    @Test
    @DisplayName("승인/반려는 대상이 없으면 잘못된 접근으로 보낸다")
    void decideWithoutTargetRedirects() throws Exception {
        mockMvc.perform(post("/proposal/document-proc")
                        .param("proposalId", "-1")
                        .param("result", "confirm")
                        .with(user(TestUsers.admin())).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-error"));
    }

    @Test
    @DisplayName("마지막 차례가 아니면 승인이 다음 결재자의 차례를 만든다")
    @Transactional
    void approveHandsOverToNextApprover() {
        Chain chain = freshChain();
        long first = proposalService.create(chain.route()[0], chain.documentId(), chain.routeId());

        var decision = proposalService.decide(
                first, chain.route()[0], "confirm", "확인했습니다.");

        assertThat(decision.message()).isEqualTo("결재완료하였습니다.");
        // 내 차례는 닫힌다 — result 0, 승인 시각이 찍힌다.
        var mine = jdbc.queryForMap(
                "SELECT result, approved_at, comment FROM proposal_tbl WHERE id = ?", first);
        assertThat(((Number) mine.get("result")).intValue()).isZero();
        assertThat(mine.get("approved_at")).isNotNull();
        assertThat(mine.get("comment")).isEqualTo("확인했습니다.");
        // 다음 차례가 결재선의 둘째 사번으로 생긴다.
        var next = jdbc.queryForMap(
                "SELECT user_tbl_id, step, result, approved_at FROM proposal_tbl "
                        + "WHERE document_tbl_id = ? AND id <> ?", chain.documentId(), first);
        assertThat(next.get("user_tbl_id")).isEqualTo(chain.route()[1]);
        assertThat(((Number) next.get("step")).intValue()).isEqualTo(1);
        assertThat(((Number) next.get("result")).intValue()).isEqualTo(3);
        assertThat(next.get("approved_at")).isNull();
    }

    @Test
    @DisplayName("결재선 끝까지 승인하면 문서의 결재가 모두 승인이 된다")
    @Transactional
    void lastApprovalMarksWholeDocumentApproved() {
        Chain chain = freshChain();
        long id = proposalService.create(chain.route()[0], chain.documentId(), chain.routeId());
        // 결재선 길이만큼 밟는다. 마지막 한 번이 문서 전체를 승인으로 바꾼다.
        // 차례마다 그 자리의 사번으로 누른다 — 이제 남의 차례는 거절된다(D-102).
        ProposalService.Decision decision = null;
        for (int step = 0; step < chain.route().length; ++step) {
            decision = proposalService.decide(id, chain.route()[step], "confirm", "확인");
            id = jdbc.queryForObject(
                    "SELECT MAX(id) FROM proposal_tbl WHERE document_tbl_id = ?",
                    Long.class, chain.documentId());
        }

        assertThat(decision.message()).isEqualTo("결재하였습니다.");
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT result FROM proposal_tbl WHERE document_tbl_id = ?", chain.documentId());
        assertThat(rows).hasSize(chain.route().length);
        assertThat(rows).allSatisfy(row ->
                assertThat(((Number) row.get("result")).intValue())
                        .isEqualTo(ProposalService.APPROVED));
    }

    @Test
    @DisplayName("승인하면 그 자리에 도장이 찍힌다")
    @Transactional
    void approvalLeavesStamp() {
        Chain chain = freshChain();
        long first = proposalService.create(chain.route()[0], chain.documentId(), chain.routeId());
        proposalService.decide(first, chain.route()[0], "confirm", "확인");

        ProposalDocument document = proposalService.document(first);

        assertThat(document.stamps()).hasSize(chain.route().length);
        // 첫 자리는 승인됐으므로 이름과 날짜(MM/dd)가 있다.
        assertThat(document.stamps().get(0).name()).isNotBlank();
        assertThat(document.stamps().get(0).approvedAt()).matches("\\d{2}/\\d{2}");
        // 둘째 자리는 결재 행은 생겼지만 아직 승인 전이라 비어 있다.
        assertThat(document.stamps().get(1).filled()).isTrue();
        assertThat(document.stamps().get(1).name()).isEmpty();
        assertThat(document.comments()).extracting(ProposalComment::comment).contains("확인");
    }

    @Test
    @DisplayName("반려하면 문서의 결재가 모두 반려가 되고 도장은 찍히지 않는다")
    @Transactional
    void rejectMarksWholeDocumentRejected() {
        Chain chain = freshChain();
        long first = proposalService.create(chain.route()[0], chain.documentId(), chain.routeId());

        var decision = proposalService.decide(
                first, chain.route()[0], "reject", "다시 올려주세요.");

        assertThat(decision.message()).isEqualTo("반려하였습니다.");
        var row = jdbc.queryForMap(
                "SELECT result, approved_at, comment FROM proposal_tbl WHERE id = ?", first);
        assertThat(((Number) row.get("result")).intValue()).isEqualTo(ProposalService.REJECTED);
        assertThat(row.get("approved_at")).isNull();
        assertThat(row.get("comment")).isEqualTo("다시 올려주세요.");
        // 반려는 다음 차례를 만들지 않는다.
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM proposal_tbl WHERE document_tbl_id = ?",
                Integer.class, chain.documentId())).isEqualTo(1);
        // 내가 올린 결재이므로 반려 탭에 뜨고 진행 중 탭에서는 사라진다(D-162).
        assertThat(idsOf(chain.route()[0], ProposalService.REJECTED)).contains(first);
        assertThat(idsOf(chain.route()[0])).doesNotContain(first);
    }

    @Test
    @DisplayName("남의 차례는 승인도 반려도 못 한다 — 레거시의 구멍을 2단계에서 막았다(D-102)")
    @Transactional
    void someoneElsesTurnIsRejected() {
        Chain chain = freshChain();
        long first = proposalService.create(chain.route()[0], chain.documentId(), chain.routeId());

        // 레거시는 결재번호만 맞으면 누구든 승인·반려할 수 있었다(D-049).
        var decision = proposalService.decide(
                first, chain.route()[1], "confirm", "가로챈 승인");

        assertThat(decision).isEqualTo(ProposalService.Decision.NOT_YOUR_TURN);
        // 결재는 그대로 진행중이고 다음 차례도 생기지 않는다.
        assertThat(jdbc.queryForObject(
                "SELECT result FROM proposal_tbl WHERE id = ?", Integer.class, first))
                .isEqualTo(ProposalService.IN_PROGRESS);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM proposal_tbl WHERE document_tbl_id = ?",
                Integer.class, chain.documentId())).isEqualTo(1);
    }

    @Test
    @DisplayName("남의 차례를 누르면 권한 화면으로 보낸다")
    @Transactional
    void someoneElsesTurnRedirectsToPermissionError() throws Exception {
        Chain chain = freshChain();
        long first = proposalService.create(chain.route()[0], chain.documentId(), chain.routeId());
        assumeTrue(!chain.route()[0].equals("admin"), "첫 차례가 admin 이라 건너뛴다");

        mockMvc.perform(post("/proposal/document-proc")
                        .param("proposalId", String.valueOf(first))
                        .param("result", "confirm")
                        .with(user(TestUsers.admin())).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/permission-error"));
    }

    /**
     * 결재를 처음부터 밟을 수 있는 문서 하나를 고른다.
     *
     * <p>결재자가 둘 이상인 결재라인을 쓰는 문서를 찾아 그 문서의 결재를 모두 지운다.
     * 롤백되는 트랜잭션 안에서만 부른다.
     */
    private Chain freshChain() {
        List<Map<String, Object>> refs = jdbc.queryForList(
                "SELECT p.document_tbl_id AS documentId, r.id AS routeId, r.route AS route "
                        + "  FROM proposal_tbl p "
                        + "  JOIN proposal_route_tbl r ON r.id = p.proposal_route_tbl_id "
                        + " WHERE r.route LIKE '%;%' LIMIT 1");
        assumeTrue(!refs.isEmpty(), "결재자가 둘 이상인 결재 데이터가 없어 건너뛴다");

        long documentId = ((Number) refs.get(0).get("documentId")).longValue();
        int routeId = ((Number) refs.get(0).get("routeId")).intValue();
        String[] route = ((String) refs.get(0).get("route")).split(";");
        jdbc.update("DELETE FROM proposal_tbl WHERE document_tbl_id = ?", documentId);
        return new Chain(documentId, routeId, route);
    }

    /** 시험용 결재 밑감. 문서 하나와 그 문서가 쓰는 결재라인. */
    private record Chain(long documentId, int routeId, String[] route) {
    }
}
