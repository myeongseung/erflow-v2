package com.erflow.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.erflow.auth.TestUsers;
import java.nio.file.Files;
import java.nio.file.Path;
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
 * 설비 화면이 실제 데이터로 그려지는지 확인한다.
 *
 * <p>연결된 MariaDB 를 상대로 돈다. 렌더링 결과는 {@code build/rendered/} 에 남긴다.
 *
 * <p>레거시 대비 요소 정합성은 {@code check_no_invention} 이 <b>템플릿 원본끼리</b>
 * 대조해 검증한다. 렌더링 결과로 대조하면 반복 영역이 데이터 개수만큼 늘어난다.
 */
@SpringBootTest(properties = "server.port=0")
@AutoConfigureMockMvc
@ActiveProfiles("local")
class UnitScreenTest {

    private static final Path OUT = Path.of("build", "rendered");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UnitService unitService;

    @BeforeAll
    static void requireLocalConfig() {
        assumeTrue(
                new ClassPathResource("application-local.yml").exists(),
                "application-local.yml 이 없어 건너뛴다");
    }

    private String render(String path, String fileName) throws Exception {
        String html = mockMvc.perform(get(path).with(user(TestUsers.admin())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Files.createDirectories(OUT);
        Files.writeString(OUT.resolve(fileName), html);
        return html;
    }

    @Test
    @DisplayName("목록이 레이아웃 안에서 실제 데이터로 그려진다")
    void listRenders() throws Exception {
        String html = render("/unit/list", "unit-list.html");

        // 레이아웃이 함께 나온다
        assertThat(html).contains("class=\"sidebar-ul\"", "생산 설비 관리");

        // 헤더 컬럼과 검색 항목
        assertThat(html).contains("장비ID", "장비명", "관리자", "장비 상태", "장비 제조일자");
        assertThat(html).contains("전체조회", "관리자명", "문서명");

        // 실제 데이터. unit_view 49건 중 첫 페이지 15건
        // id 가 varchar 라 문자열 정렬이다 — A-10 이 A-5 보다 앞선다. 레거시도 같다.
        UnitRow first = unitService.list(UnitSearch.none(), 1).rows().get(0);
        assertThat(html).contains("<td>" + first.id() + "</td>");

        int rows = html.split("name=\"unitId\"", -1).length - 1;
        assertThat(rows).as("페이지당 15건").isEqualTo(15);
    }

    @Test
    @DisplayName("페이징이 레거시와 같은 페이지 수를 그린다")
    void listPaging() throws Exception {
        String html = render("/unit/list?nowPage=2", "unit-list-p2.html");

        // 49건 -> 4페이지, 1블록. 이전/다음 블록 링크는 없다
        assertThat(html).contains("javascript:paging(1)", "javascript:paging(4)");
        assertThat(html).doesNotContain(">이전<", ">다음<");

        UnitService.UnitPage page = unitService.list(UnitSearch.none(), 2);
        assertThat(page.pagination().totalRecord()).isEqualTo(49);
        assertThat(page.rows()).hasSize(15);
    }

    @Test
    @DisplayName("검색이 화이트리스트 컬럼으로만 걸린다")
    void searchUsesWhitelist() {
        UnitService.UnitPage all = unitService.list(UnitSearch.none(), 1);
        UnitService.UnitPage byName =
                unitService.list(new UnitSearch("name", "해동기"), 1);
        assertThat(byName.pagination().totalRecord())
                .isLessThan(all.pagination().totalRecord());

        // 레거시는 화이트리스트에 없는 keyfield 를 조건으로 만들지 않았다
        UnitService.UnitPage injected = unitService.list(
                new UnitSearch("id; DROP TABLE unit_tbl --", "x"), 1);
        assertThat(injected.pagination().totalRecord())
                .as("조건이 무시되어 전체 조회로 떨어진다")
                .isEqualTo(all.pagination().totalRecord());
    }

    @Test
    @DisplayName("등록 팝업에는 레이아웃이 없다")
    void registerIsPopup() throws Exception {
        String html = render("/unit/register", "unit-register.html");

        assertThat(html).contains("장비ID", "관리자ID", "문서ID", "장비명",
                "관리자 선택", "문서 선택", "제출");
        // 레거시도 이 화면에는 헤더·사이드메뉴를 넣지 않았다
        assertThat(html).doesNotContain("sidebar-ul", "navbar-brand");
    }

    @Test
    @DisplayName("수정 팝업이 기존 값을 채워 그린다")
    void updateShowsCurrentValues() throws Exception {
        UnitRow first = unitService.list(UnitSearch.none(), 1).rows().get(0);
        Unit unit = unitService.get(first.id());
        assertThat(unit).isNotNull();

        String html = render("/unit/update?id=" + first.id(), "unit-update.html");

        assertThat(html).contains("관리자 사번", "문서번호", "장비명", "멈춤", "가동중", "제출");
        assertThat(html).contains("value=\"" + unit.id() + "\"");
        assertThat(html).contains("value=\"" + unit.name() + "\"");
        assertThat(html).doesNotContain("sidebar-ul");
    }

    @Test
    @DisplayName("없는 설비를 수정하려 하면 잘못된 접근으로 보낸다")
    void updateRejectsUnknownId() throws Exception {
        mockMvc.perform(get("/unit/update?id=__없는설비__").with(user(TestUsers.admin())))
                .andExpect(status().is3xxRedirection());
    }
}
