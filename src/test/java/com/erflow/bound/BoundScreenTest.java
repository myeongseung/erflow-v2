package com.erflow.bound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.erflow.auth.TestUsers;
import java.nio.file.Files;
import java.nio.file.Path;
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
 * 입·출고 목록이 실제 데이터로 그려지는지 확인한다.
 *
 * <p>입고(type=0)와 출고(type=1)가 서로 다른 목록을 보여주는지, 검색이 대상 칸만
 * 훑는지를 주로 본다.
 */
@SpringBootTest(properties = "server.port=0")
@AutoConfigureMockMvc
@ActiveProfiles("local")
class BoundScreenTest {

    private static final Path OUT = Path.of("build", "rendered");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BoundService boundService;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeAll
    static void requireLocalConfig() {
        assumeTrue(
                new ClassPathResource("application-local.yml").exists(),
                "application-local.yml 이 없어 건너뛴다");
    }

    private String render(String path, String fileName) throws Exception {
        String html = mockMvc.perform(get(path).with(user(TestUsers.admin())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Files.createDirectories(OUT);
        Files.writeString(OUT.resolve(fileName), html);
        return html;
    }

    @Test
    @DisplayName("입고 목록이 그려지고 화면 글자가 입고다")
    void inboundListRenders() throws Exception {
        String html = render("/bound/inbound", "bound-inbound.html");

        assertThat(html).contains("입고 제품 관리").contains("입고 시간").contains("입고자");
        assertThat(html).doesNotContain("출고 시간");
    }

    @Test
    @DisplayName("출고 목록은 화면 글자가 출고다")
    void outboundListRenders() throws Exception {
        String html = render("/bound/outbound", "bound-outbound.html");

        assertThat(html).contains("출고 제품 관리").contains("출고 시간").contains("출고자");
    }

    @Test
    @DisplayName("입고와 출고는 서로 다른 목록이다")
    void inboundAndOutboundAreDifferentSets() {
        List<BoundRow> in = boundService.list(BoundService.INBOUND, BoundSearch.none(), 1).rows();
        List<BoundRow> out = boundService.list(BoundService.OUTBOUND, BoundSearch.none(), 1).rows();
        assumeTrue(!in.isEmpty() || !out.isEmpty(), "입·출고 데이터가 없어 건너뛴다");

        List<Integer> outIds = out.stream().map(BoundRow::id).toList();
        assertThat(in).noneMatch(row -> outIds.contains(row.id()));
    }

    @Test
    @DisplayName("제품명으로 걸러낸다")
    void filtersByProductName() {
        var all = boundService.list(BoundService.INBOUND, BoundSearch.none(), 1).rows();
        assumeTrue(!all.isEmpty(), "입고 데이터가 없어 건너뛴다");
        String name = all.get(0).productName();

        var filtered = boundService.list(
                BoundService.INBOUND, new BoundSearch("productName", name), 1).rows();

        assertThat(filtered).isNotEmpty()
                .allSatisfy(row -> assertThat(row.productName()).contains(name));
    }

    @Test
    @DisplayName("화이트리스트 밖 keyfield 는 전체 조회로 떨어진다")
    void unknownKeyfieldFallsBackToAll() {
        int all = boundService.list(BoundService.INBOUND, BoundSearch.none(), 1)
                .pagination().totalRecord();
        int junk = boundService.list(
                BoundService.INBOUND, new BoundSearch("id2; DROP TABLE bound_tbl --", "x"), 1)
                .pagination().totalRecord();

        assertThat(junk).isEqualTo(all);
    }

    @Test
    @DisplayName("입고 등록 화면이 그려진다")
    void inboundRegisterFormRenders() throws Exception {
        String html = render("/bound/register?flag=inbound", "bound-register.html");

        assertThat(html).contains("입고 등록").contains("제품 찾기").contains("우편번호 찾기");
    }

    @Test
    @DisplayName("flag 가 없으면 잘못된 접근으로 보낸다")
    void registerWithoutFlagRedirects() throws Exception {
        // /bound/register 는 screen 테이블에 flag=inbound·outbound 만 있어, flag 없는
        // 요청은 화면 권한 판정이 컨트롤러 전에 막는다.
        mockMvc.perform(get("/bound/register").with(user(TestUsers.admin())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("등록하면 그 type 으로 bound_tbl 에 한 건 쌓인다")
    @Transactional
    void createInsertsWithType() throws Exception {
        // FK 안전한 값을 기존 입고 한 건에서 가져온다(제품·사번이 실재해야 한다). 롤백된다.
        var refs = jdbc.queryForList(
                "SELECT product_tbl_id, user_tbl_id FROM bound_tbl WHERE type = 0 LIMIT 1");
        assumeTrue(!refs.isEmpty(), "입고 데이터가 없어 건너뛴다");
        String productId = (String) refs.get(0).get("product_tbl_id");
        String userId = (String) refs.get(0).get("user_tbl_id");
        int before = jdbc.queryForObject(
                "SELECT COUNT(*) FROM bound_tbl WHERE type = 1", Integer.class);

        boolean created = boundService.create(new Bound(
                productId, userId, "12345", "도로명", "상세", "2026-01-01 00:00:00", 7, 1));

        assertThat(created).isTrue();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM bound_tbl WHERE type = 1", Integer.class))
                .isEqualTo(before + 1);
    }

    @Test
    @DisplayName("입고 수정 화면이 기존 값으로 그려진다")
    void inboundUpdateFormRenders() throws Exception {
        Integer id = jdbc.queryForObject(
                "SELECT id FROM bound_tbl WHERE type = 0 LIMIT 1", Integer.class);
        assumeTrue(id != null, "입고 데이터가 없어 건너뛴다");

        String html = render("/bound/update?flag=inbound&id=" + id, "bound-update.html");

        assertThat(html).contains("입고 수정").contains("제출").contains("우편 찾기");
    }

    @Test
    @DisplayName("수정 조회의 사번 자리에 진짜 사번이 채워진다 — D-048 을 2단계에서 고쳤다(D-103)")
    void updateDetailCarriesTheRealUserId() throws Exception {
        Integer id = jdbc.queryForObject(
                "SELECT id FROM bound_tbl WHERE type = 0 LIMIT 1", Integer.class);
        assumeTrue(id != null, "입고 데이터가 없어 건너뛴다");

        BoundDetail bound = boundService.get(id, BoundService.INBOUND);

        // 레거시는 이 자리에 product_id 를 읽어(D-048) 직원을 다시 고르지 않고
        // 저장하면 담당 사번이 제품 코드로 덮였다. 이제 저장한 사번 그대로다.
        String stored = jdbc.queryForObject(
                "SELECT user_tbl_id FROM bound_tbl WHERE id = ?", String.class, id);
        assertThat(bound.userId()).isEqualTo(stored);
        assertThat(bound.userId()).isNotEqualTo(bound.productId());
    }

    @Test
    @DisplayName("삭제는 type 이 맞아야 지운다")
    @Transactional
    void deleteRespectsType() {
        Integer inId = jdbc.queryForObject(
                "SELECT id FROM bound_tbl WHERE type = 0 LIMIT 1", Integer.class);
        assumeTrue(inId != null, "입고 데이터가 없어 건너뛴다");

        // 입고 건을 출고 type 으로 지우려 하면 걸리는 행이 없다.
        assertThat(boundService.delete(List.of(inId), BoundService.OUTBOUND)).isFalse();
        assertThat(boundService.delete(List.of(inId), BoundService.INBOUND)).isTrue();
    }
}
