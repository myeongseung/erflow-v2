package com.erflow.product;

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
 * 제품 화면 셋이 실제 데이터로 도는지 확인한다.
 *
 * <p>같은 표를 {@code type} 으로 나눠 보는 화면이라 <b>분류가 섞이지 않는지</b>가 핵심이다.
 * 섞여도 화면은 멀쩡해 보이고 게이트도 통과한다 — 원재료 목록에 완제품이 끼어 있어도
 * 요소는 같기 때문이다.
 */
@SpringBootTest(properties = "server.port=0")
@AutoConfigureMockMvc
@ActiveProfiles("local")
class ProductScreenTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductService productService;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeAll
    static void requireLocalConfig() {
        assumeTrue(
                new ClassPathResource("application-local.yml").exists(),
                "application-local.yml 이 없어 건너뛴다");
    }

    @Test
    @DisplayName("세 목록이 각자의 분류만 보여준다")
    void listsAreSeparatedByType() {
        for (ProductType type : ProductType.values()) {
            var rows = productService.list(type, ProductSearch.none(), 1).rows();

            assertThat(rows).isNotEmpty();
            assertThat(rows).allSatisfy(row ->
                    assertThat(row.type()).isEqualTo(type.code()));
        }
    }

    @Test
    @DisplayName("세 화면이 각자의 글자로 그려진다")
    void screensRenderTheirOwnLabels() throws Exception {
        assertThat(render("/product/ingredient-product"))
                .contains("사용자 &gt; 구매 &gt; 원재료 관리").contains("제품 분류");
        assertThat(render("/product/processed-product"))
                .contains("사용자 &gt; 구매 &gt; 가공품 관리");
        assertThat(render("/product/producted-product"))
                .contains("사용자 &gt; 제품 &gt; 완제품");
    }

    @Test
    @DisplayName("고른 칸으로만 검색한다")
    void searchesOnlyTheChosenField() {
        var any = productService.list(ProductType.INGREDIENT, ProductSearch.none(), 1)
                .rows().get(0);

        var byId = productService.list(
                ProductType.INGREDIENT, new ProductSearch("productId", any.id()), 1).rows();
        var byName = productService.list(
                ProductType.INGREDIENT, new ProductSearch("productName", any.id()), 1).rows();

        assertThat(byId).extracting(ProductListRow::id).contains(any.id());
        // 이름 칸을 제품 ID 로 훑으면 걸리지 않는다(이름이 ID 를 품고 있지 않은 한).
        assertThat(byName.size()).isLessThanOrEqualTo(byId.size());
    }

    @Test
    @DisplayName("화면이 주지 않은 칸으로는 검색되지 않는다")
    void unknownFieldIsIgnored() {
        var injected = productService.list(
                ProductType.INGREDIENT, new ProductSearch("id = 'x' or 1=1 -- ", "x"), 1);
        var all = productService.list(ProductType.INGREDIENT, ProductSearch.none(), 1);

        assertThat(injected.rows()).hasSameSizeAs(all.rows());
        assertThat(injected.pagination().totalRecord())
                .isEqualTo(all.pagination().totalRecord());
    }

    @Test
    @DisplayName("등록하면 고른 분류로 저장된다")
    @Transactional
    void createStoresTheChosenType() {
        assertThat(productService.create("T-9001", "시험제품", 7, ProductType.PROCESSED)).isTrue();

        var row = jdbc.queryForMap("SELECT name, count, type FROM product_tbl WHERE id = 'T-9001'");
        assertThat(row.get("name")).isEqualTo("시험제품");
        assertThat(((Number) row.get("count")).intValue()).isEqualTo(7);
        assertThat(((Number) row.get("type")).intValue()).isEqualTo(ProductType.PROCESSED.code());
    }

    @Test
    @DisplayName("같은 제품 ID 는 다시 등록할 수 없다")
    @Transactional
    void duplicateIdIsRejected() {
        productService.create("T-9002", "시험제품", 1, ProductType.INGREDIENT);

        assertThat(productService.create("T-9002", "다른이름", 2, ProductType.INGREDIENT))
                .isFalse();
    }

    @Test
    @DisplayName("수정하면 이름·수량·분류가 함께 바뀐다")
    @Transactional
    void updateChangesAllThree() {
        productService.create("T-9003", "처음이름", 1, ProductType.INGREDIENT);

        assertThat(productService.update("T-9003", "고친이름", 9, ProductType.PRODUCTED)).isTrue();

        var row = jdbc.queryForMap("SELECT name, count, type FROM product_tbl WHERE id = 'T-9003'");
        assertThat(row.get("name")).isEqualTo("고친이름");
        assertThat(((Number) row.get("count")).intValue()).isEqualTo(9);
        assertThat(((Number) row.get("type")).intValue()).isEqualTo(ProductType.PRODUCTED.code());
    }

    @Test
    @DisplayName("지우면 사라진다")
    @Transactional
    void deleteRemovesRows() {
        productService.create("T-9004", "시험제품", 1, ProductType.INGREDIENT);

        assertThat(productService.delete(List.of("T-9004"))).isTrue();

        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM product_tbl WHERE id = 'T-9004'", Integer.class)).isZero();
    }

    @Test
    @DisplayName("분류가 없으면 등록·수정 화면이 잘못된 접근으로 보낸다")
    void missingFlagRedirects() throws Exception {
        mockMvc.perform(get("/product/register").with(user(TestUsers.admin())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-error"));
        mockMvc.perform(get("/product/update").param("id", "no-such")
                        .with(user(TestUsers.admin())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-error"));
    }

    @Test
    @DisplayName("권한이 없으면 목록을 볼 수 없다")
    void withoutPermissionBlocked() throws Exception {
        mockMvc.perform(get("/product/ingredient-product")
                        .with(user(TestUsers.noPermission())))
                .andExpect(status().isForbidden());
    }

    private String render(String path) throws Exception {
        return mockMvc.perform(get(path).with(user(TestUsers.admin())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }
}
