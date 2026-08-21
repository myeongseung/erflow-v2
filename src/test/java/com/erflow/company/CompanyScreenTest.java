package com.erflow.company;

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
 * 협력업체 화면이 실제 데이터로 그려지는지 확인한다.
 *
 * <p>구매(flag=1)와 영업(flag=0)이 같은 화면을 쓰므로 두 갈래를 모두 본다.
 */
@SpringBootTest(properties = "server.port=0")
@AutoConfigureMockMvc
@ActiveProfiles("local")
class CompanyScreenTest {

    private static final Path OUT = Path.of("build", "rendered");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompanyService companyService;

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
    @DisplayName("구매와 영업이 같은 화면에서 다른 목록을 그린다")
    void listSplitsByFlag() throws Exception {
        String purchase = render("/company/list?flag=1", "company-list-purchase.html");
        String sales = render("/company/list?flag=0", "company-list-sales.html");

        assertThat(purchase).contains("사용자 &gt; 구매 &gt; 협력업체 관리");
        assertThat(sales).contains("사용자 &gt; 영업 &gt; 협력업체 관리");

        // 레거시 데이터는 구매 15 / 영업 15 다
        assertThat(companyService.list(Company.INBOUND, CompanySearch.none(), 1)
                .pagination().totalRecord()).isEqualTo(15);
        assertThat(companyService.list(Company.OUTBOUND, CompanySearch.none(), 1)
                .pagination().totalRecord()).isEqualTo(15);

        // 목록 링크가 flag 를 달고 다녀야 권한이 갈린다 (D-016)
        assertThat(purchase).contains("flag=1");
        assertThat(sales).contains("flag=0");
    }

    @Test
    @DisplayName("목록이 업종코드가 아니라 업종명을 보여준다")
    void listShowsFieldName() throws Exception {
        String html = render("/company/list?flag=1", "company-list-purchase.html");

        Company first = companyService.list(Company.INBOUND, CompanySearch.none(), 1).rows().get(0);
        String fieldName = companyService.fieldName(first.field());
        assertThat(fieldName).as("표본 업체의 업종코드가 코드표에 있어야 한다").isNotNull();
        assertThat(html).contains(fieldName);
    }

    @Test
    @DisplayName("협력업체명으로 검색한다")
    void searchByName() {
        var all = companyService.list(Company.INBOUND, CompanySearch.none(), 1);
        Company sample = all.rows().get(0);

        var found = companyService.list(
                Company.INBOUND, companyService.toSearch("name", sample.name()), 1);

        assertThat(found.pagination().totalRecord()).isBetween(1, all.pagination().totalRecord());
        assertThat(found.rows()).extracting(Company::name).contains(sample.name());
    }

    @Test
    @DisplayName("업체 종목으로 검색하면 업종명을 코드로 풀어 건다")
    void searchByField() {
        CompanySearch search = companyService.toSearch("field", "제조업");
        assertThat(search.byField()).isTrue();
        assertThat(search.fieldCodes()).isNotEmpty();

        var found = companyService.list(Company.INBOUND, search, 1);
        assertThat(found.rows()).allSatisfy(company ->
                assertThat(search.fieldCodes()).contains(company.field()));
    }

    @Test
    @DisplayName("걸리는 업종이 없으면 결과가 없다 — 레거시는 여기서 죽었다")
    void searchByFieldWithNoMatch() {
        // 레거시: sb 가 비어 "".substring(1) 로 StringIndexOutOfBoundsException (D-017)
        CompanySearch search = companyService.toSearch("field", "존재하지않는업종명");
        assertThat(search.matchesNothing()).isTrue();

        assertThat(companyService.list(Company.INBOUND, search, 1).pagination().totalRecord())
                .isZero();
    }

    @Test
    @DisplayName("등록·수정 화면에는 레이아웃이 들어간다")
    void formsHaveLayout() throws Exception {
        String register = render("/company/register?flag=1", "company-register.html");
        Company first = companyService.list(Company.INBOUND, CompanySearch.none(), 1).rows().get(0);
        String update = render("/company/update?flag=1&id=" + first.id(), "company-update.html");

        // unit 과 달리 팝업이 아니다. 레거시도 여기에는 include 를 했다.
        assertThat(register).contains("sidebar-ul", "navbar-brand");
        assertThat(update).contains("sidebar-ul", "navbar-brand");

        assertThat(register).contains("회사명", "우편 찾기", "업체 찾기", "은행 찾기", "제출");
        assertThat(update).contains("상청", "하청", first.name());
    }

    @Test
    @DisplayName("없는 협력업체를 수정하려 하면 잘못된 접근으로 보낸다")
    void updateRejectsUnknownId() throws Exception {
        mockMvc.perform(get("/company/update?flag=1&id=999999").with(user(TestUsers.admin())))
                .andExpect(status().is3xxRedirection());
    }
}
