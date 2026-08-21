package com.erflow.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.erflow.auth.TestUsers;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
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
 * 찾기 팝업.
 *
 * <p>코드 찾기(은행·업종·문서)에서는 검색 전과 «검색어를 비운 채 누른 것»이 다르다는
 * 점을 주로 본다. 레거시가 그렇게 갈라 놓았고, 눈으로는 잘 드러나지 않는 차이다.
 *
 * <p>사용자 찾기는 도움말이 없고 열자마자 전체 목록이 나온다. 걸러내는 조건이 셋이라
 * 어느 칸을 보고 어느 칸을 안 보는지를 확인한다 — 표에 사번이 보이는데 검색은 이름만
 * 훑는다.
 */
@SpringBootTest(properties = "server.port=0")
@AutoConfigureMockMvc
@ActiveProfiles("local")
class FindPopupTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeAll
    static void requireLocalConfig() {
        assumeTrue(
                new ClassPathResource("application-local.yml").exists(),
                "application-local.yml 이 없어 건너뛴다");
    }

    /**
     * 팝업을 연다. 검색어는 파라미터로 넘긴다 — 주소에 한글을 그대로 박으면
     * MockMvc 가 ISO-8859-1 로 풀어 글자가 깨진다. 실제 Tomcat 은 UTF-8 로 푼다.
     *
     * @param path 경로
     * @param search 검색어. {@code null} 이면 파라미터를 아예 보내지 않는다
     * @return 받은 HTML
     */
    private String open(String path, String search) throws Exception {
        var request = get(path).with(user(TestUsers.admin()));
        if (search != null) {
            request = request.param("search", search);
        }
        return mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    @DisplayName("창을 처음 열면 도움말이 나온다")
    void firstOpenShowsTips() throws Exception {
        String html = open("/find/bank", null);

        assertThat(html).contains("tip").contains("산업은행");
        assertThat(html).doesNotContain("a-bank-info");
    }

    @Test
    @DisplayName("검색어를 비운 채 누르면 전체 목록이 나온다")
    void emptySearchShowsEverything() throws Exception {
        // 레거시는 search 파라미터가 없을 때와 빈 문자열일 때를 구별한다.
        String html = open("/find/bank", "");

        assertThat(html).contains("a-bank-info");
        assertThat(html).doesNotContain("<h3>tip</h3>");
    }

    @Test
    @DisplayName("은행 이름으로 찾는다")
    void findsBankByName() throws Exception {
        String html = open("/find/bank", "기업은행");

        assertThat(html).contains("기업은행");
    }

    @Test
    @DisplayName("은행 코드로도 찾는다")
    void findsBankByCode() throws Exception {
        // CodeTable.matching 은 이름만 보지만 팝업은 코드도 훑어야 한다.
        String html = open("/find/bank", "003");

        assertThat(html).contains("003");
        assertThat(html).contains("a-bank-info");
    }

    @Test
    @DisplayName("업종 팝업은 값을 data 속성으로 넘긴다")
    void workPopupUsesDataAttributes() throws Exception {
        String html = open("/find/work", "어업");

        // 은행 팝업은 onclick, 업종 팝업은 data-key/data-value 다. 레거시 그대로다.
        assertThat(html).contains("a-work-info").contains("data-key=");
    }

    @Test
    @DisplayName("문서 찾기는 도움말이 뜨지 않는다")
    void documentPopupNeverShowsTips() throws Exception {
        // 레거시가 검색어를 빈 문자열로 채워 놓고 null 인지 검사한다. 도움말
        // 갈래가 한 번도 실행되지 않는다 — 결함이지만 그대로 옮겼다. D-036 참조.
        String html = open("/find/document", null);

        assertThat(html).doesNotContain("<h3>tip</h3>");
    }

    @Test
    @DisplayName("찾는 것이 없으면 목록만 비고 화면은 뜬다")
    void noMatchStillRenders() throws Exception {
        String html = open("/find/work", "없는업종이름12345");

        assertThat(html).doesNotContain("a-work-info");
        assertThat(html).contains("업체 코드 찾기 서비스 제공");
    }

    /**
     * 목록에서 (코드, 이름) 짝을 모은다. 팝업이 값을 data 속성으로 넘긴다(D-035).
     *
     * @param html 받은 HTML
     * @return 코드를 키로 하는 짝. 순서는 화면 순서다
     */
    private static Map<String, String> pairs(String html) {
        var found = new LinkedHashMap<String, String>();
        var pair = Pattern.compile("data-key=\"([^\"]*)\"\\s+data-value=\"([^\"]*)\"")
                .matcher(html);
        while (pair.find()) {
            found.put(pair.group(1), pair.group(2));
        }
        return found;
    }

    @Test
    @DisplayName("협력업체 찾기도 처음 열면 도움말이다")
    void companyPopupShowsTipsFirst() throws Exception {
        String html = open("/find/company", null);

        assertThat(html).contains("<h3>tip</h3>");
        assertThat(html).doesNotContain("a_company_info");
    }

    @Test
    @DisplayName("협력업체는 번호로 찾지 못한다")
    void companyPopupCannotSearchById() throws Exception {
        // 도움말은 «협력업체 ID — 예) 1 -> 삼성» 이라고 안내한다. 그런데 조건은
        // 이름만 훑는다 — 은행·업종·제품 팝업은 코드도 본다. 그대로 옮겼다(D-039).
        Map<String, String> all = pairs(open("/find/company", ""));
        // 이름에 자기 번호가 든 업체를 고르면 이름 검색으로 걸려 버린다. 그런 짝은 건넌다.
        String id = all.entrySet().stream()
                .filter(entry -> !entry.getValue().contains(entry.getKey()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new AssertionError("번호가 이름에 없는 업체가 없다"));

        assertThat(pairs(open("/find/company", id))).doesNotContainKey(id);
    }

    @Test
    @DisplayName("제품은 코드로도 찾는다")
    void productPopupSearchesByCode() throws Exception {
        Map<String, String> all = pairs(open("/find/product", ""));
        String code = all.keySet().iterator().next();

        assertThat(pairs(open("/find/product", code))).containsKey(code);
    }

    @Test
    @DisplayName("여러 제품 찾기는 검색한 뒤에만 표가 나온다")
    void multiProductPopupShowsTableAfterSearch() throws Exception {
        assertThat(open("/find/multi-product", null))
                .contains("<h3>tip</h3>")
                .doesNotContain("search-people-body-product-id");

        String html = open("/find/multi-product", "");

        assertThat(column(html, "search-people-body-product-id")).isNotEmpty();
        assertThat(html).doesNotContain("<h3>tip</h3>");
    }

    @Test
    @DisplayName("두 제품 팝업은 같은 제품을 같은 순서로 보여준다")
    void bothProductPopupsShareTheSameQuery() throws Exception {
        List<String> one = List.copyOf(pairs(open("/find/product", "")).keySet());
        List<String> many = column(open("/find/multi-product", ""),
                "search-people-body-product-id");

        assertThat(many).isNotEmpty().isEqualTo(one);
    }

    @Test
    @DisplayName("결재라인 찾기의 도움말은 협력업체 이야기다")
    void proposalRoutePopupShowsCompanyTips() throws Exception {
        // 레거시가 협력업체 찾기를 복사해 만들면서 도움말도 꼬리말도 고치지 않았다.
        // 결재라인 화면인데 «협력업체 ID» 라고 안내한다. 그대로 옮겼다.
        String html = open("/find/proposal-route", null);

        assertThat(html).contains("<h3>tip</h3>").contains("협력업체 ID");
        assertThat(html).contains("© 협력업체 찾기 서비스 제공");
    }

    @Test
    @DisplayName("남이 만든 결재라인은 보이지 않는다")
    void proposalRoutePopupShowsOnlyMine() throws Exception {
        // 조회가 사번으로 좁혀져 있다. 결재라인이 하나도 없는 사람에게는 아무것도
        // 나오지 않아야 한다 — 좁히지 않았다면 남의 것이 전부 나온다.
        String nobody = mockMvc.perform(get("/find/proposal-route")
                        .with(user(TestUsers.noPermission())).param("search", ""))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(pairs(nobody)).isEmpty();
        // admin 계정에도 결재라인이 있다. 좁히는 것과 비어 있는 것은 다르다.
        assertThat(pairs(open("/find/proposal-route", ""))).isNotEmpty();
    }

    @Test
    @DisplayName("결재라인은 이름으로만 찾는다")
    void proposalRoutePopupSearchesByNameOnly() throws Exception {
        Map<String, String> all = pairs(open("/find/proposal-route", ""));
        String id = all.keySet().stream()
                .findFirst()
                .orElseThrow(() -> new AssertionError("결재라인이 없다"));

        // 번호로 찾으면 나오지 않는다. 협력업체 팝업과 같다(D-039).
        assertThat(pairs(open("/find/proposal-route", id))).doesNotContainKey(id);
    }

    @Test
    @DisplayName("결재라인이 돌려주는 값은 사번을 이은 문자열이다")
    void proposalRoutePopupHandsBackTheRouteString() throws Exception {
        // 레거시는 이 문자열을 쪼개 사용자마다 조회한 뒤 다시 이어 붙였다. 컬럼을
        // 그대로 읽으므로 빈 조각이 없는지 확인한다 — D-041 참조.
        assertThat(pairs(open("/find/proposal-route", "")).values())
                .isNotEmpty()
                .allSatisfy(route -> assertThat(route).matches("[^;]+(;[^;]+)*"));
    }

    /**
     * 사용자 찾기 팝업을 연다.
     *
     * @param dept 부서명. 빈 문자열이면 전체부서
     * @param job 직급명. 빈 문자열이면 전체직급
     * @param keyword 이름 검색어
     * @return 받은 HTML
     */
    private String openUsers(String dept, String job, String keyword) throws Exception {
        return mockMvc.perform(get("/find/user").with(user(TestUsers.admin()))
                        .param("deptKeyfield", dept)
                        .param("jobKeyfield", job)
                        .param("keyword", keyword))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    /**
     * 표에서 한 칸의 글자를 모은다. 셀마다 class 가 달려 있어 그것으로 고른다.
     *
     * @param html 받은 HTML
     * @param cellClass 셀의 class 이름
     * @return 그 칸의 값 목록
     */
    private static List<String> column(String html, String cellClass) {
        var found = new ArrayList<String>();
        var cell = Pattern.compile("class=\"" + cellClass + "\">([^<]*)<").matcher(html);
        while (cell.find()) {
            found.add(cell.group(1).strip());
        }
        return found;
    }

    /**
     * 콤보의 항목을 모은다. «전체부서» 처럼 값이 빈 항목은 뺀다.
     *
     * @param html 받은 HTML
     * @param selectClass select 의 class 이름
     * @return 고를 수 있는 값 목록
     */
    private static List<String> options(String html, String selectClass) {
        var block = Pattern.compile("class=\"" + selectClass + "\".*?</select>", Pattern.DOTALL)
                .matcher(html);
        if (!block.find()) {
            return List.of();
        }
        var found = new ArrayList<String>();
        var option = Pattern.compile("<option value=\"([^\"]+)\"").matcher(block.group());
        while (option.find()) {
            found.add(option.group(1));
        }
        return found;
    }

    @Test
    @DisplayName("사용자 팝업은 열자마자 전체 목록이 나온다")
    void userPopupListsEveryoneOnOpen() throws Exception {
        // 코드 찾기 셋과 다르다. 도움말 마크업이 아예 없고 검색 전후를 가리지 않는다.
        String html = mockMvc.perform(get("/find/user").with(user(TestUsers.admin())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).doesNotContain("tip");
        assertThat(column(html, "search-people-body-receiver")).isNotEmpty();
    }

    @Test
    @DisplayName("관리자는 목록에 없다")
    void userPopupHidesAdmin() throws Exception {
        // 레거시 조건이 where id <> 'admin' 이다.
        String html = openUsers("", "", "");

        assertThat(column(html, "search-people-body-receiver-id")).doesNotContain("admin");
    }

    @Test
    @DisplayName("이름으로 걸러낸다")
    void userPopupFiltersByName() throws Exception {
        String someone = column(openUsers("", "", ""), "search-people-body-receiver").get(0);

        List<String> names =
                column(openUsers("", "", someone), "search-people-body-receiver");

        assertThat(names).isNotEmpty().allSatisfy(name -> assertThat(name).contains(someone));
    }

    @Test
    @DisplayName("사번으로는 찾지 못한다")
    void userPopupDoesNotSearchById() throws Exception {
        // 표에 사번이 보이지만 레거시 조건은 name like 뿐이다. 그대로 옮겼다.
        String someone = column(openUsers("", "", ""), "search-people-body-receiver-id").get(0);

        String html = openUsers("", "", someone);

        assertThat(column(html, "search-people-body-receiver-id")).isEmpty();
    }

    @Test
    @DisplayName("부서를 고르면 그 부서만 남고 콤보에 선택이 남는다")
    void userPopupFiltersByDeptAndKeepsSelection() throws Exception {
        String all = openUsers("", "", "");
        List<String> combo = options(all, "search-people-dept");
        // 콤보에 있는 부서 중 실제로 사용자가 있는 것을 고른다. 콤보에 없는 부서를
        // 가진 사용자가 있어서(관리자 부서, D-038) 표의 첫 줄을 그냥 쓸 수 없다.
        String dept = column(all, "search-people-body-dept").stream()
                .filter(combo::contains)
                .findFirst()
                .orElseThrow(() -> new AssertionError("콤보에 있는 부서를 가진 사용자가 없다"));

        String html = openUsers(dept, "", "");

        assertThat(column(html, "search-people-body-dept"))
                .isNotEmpty().allSatisfy(name -> assertThat(name).contains(dept));
        // 다시 열었을 때 고른 부서가 콤보에 남아 있어야 한다. 레거시가
        // deptKeyfield.equals(name) 로 selected 를 찍는다 — 딱 하나여야 한다.
        assertThat(html).containsOnlyOnce("selected=\"selected\"");
    }

    @Test
    @DisplayName("개별 찾기는 라디오로 한 명만 고른다")
    void eachUserPopupPicksOne() throws Exception {
        String html = mockMvc.perform(get("/find/each-user").with(user(TestUsers.admin())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("type=\"radio\"");
        // 전체 선택 칸이 없다. 한 명만 고르는 화면이라 있을 이유가 없다.
        assertThat(html).doesNotContain("chkAll");
    }

    @Test
    @DisplayName("두 사용자 팝업은 같은 사람들을 보여준다")
    void bothUserPopupsShareTheSameQuery() throws Exception {
        // 조회는 하나다. 고르는 방식만 다르다 — 화면 둘이 어긋나면 그쪽이 갈라진 것이다.
        String many = mockMvc.perform(get("/find/user").with(user(TestUsers.admin())))
                .andReturn().getResponse().getContentAsString();
        String one = mockMvc.perform(get("/find/each-user").with(user(TestUsers.admin())))
                .andReturn().getResponse().getContentAsString();

        assertThat(column(one, "search-people-body-receiver-id"))
                .isNotEmpty()
                .isEqualTo(column(many, "search-people-body-receiver-id"));
    }

    @Test
    @DisplayName("관리자 부서는 콤보에 없다")
    void adminDeptIsHiddenFromCombo() throws Exception {
        // 레거시 DepartmentService 주석: «불러올 때 관리자를 제외하고 불러올 것
        // (관리자 부서 번호: -1)». 목록 쪽은 admin 계정만 빼므로 그 부서에 속한
        // 사용자는 표에 나온다. 좁힐 수 없는 부서가 표에 보이는 셈이다 — D-038.
        String html = openUsers("", "", "");

        assertThat(options(html, "search-people-dept")).isNotEmpty().doesNotContain("관리자");
        assertThat(options(html, "search-people-job")).isNotEmpty().doesNotContain("관리자");
    }
}
