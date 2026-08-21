package com.erflow.proposal;

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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결재라인 목록이 실제 데이터로 그려지는지 확인한다.
 *
 * <p>결재경로(사번을 ; 로 이은 문자열)를 사람마다 «[부서/직급] 이름(사번)» 로 풀어
 * 보여주는지, 내 결재라인만 나오는지를 주로 본다.
 */
@SpringBootTest(properties = "server.port=0")
@AutoConfigureMockMvc
@ActiveProfiles("local")
class ProposalRouteScreenTest {

    private static final Path OUT = Path.of("build", "rendered");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProposalRouteService proposalRouteService;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeAll
    static void requireLocalConfig() {
        assumeTrue(
                new ClassPathResource("application-local.yml").exists(),
                "application-local.yml 이 없어 건너뛴다");
    }

    @Test
    @DisplayName("결재라인 목록이 그려진다")
    void routeListRenders() throws Exception {
        String html = mockMvc.perform(get("/proposal/route-list").with(user(TestUsers.admin())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Files.createDirectories(OUT);
        Files.writeString(OUT.resolve("proposal-route-list.html"), html);

        assertThat(html).contains("결재라인 관리").contains("결재관리번호").contains("결재경로");
    }

    @Test
    @DisplayName("결재경로가 사람마다 [부서/직급] 이름(사번) 으로 풀린다")
    void routeChainIsResolved() {
        // 결재라인을 하나 가진 사용자를 찾는다.
        List<String> owners = jdbc.queryForList(
                "SELECT DISTINCT user_id FROM proposal_route_view LIMIT 5", String.class);
        assumeTrue(!owners.isEmpty(), "결재라인 데이터가 없어 건너뛴다");

        for (String owner : owners) {
            var items = proposalRouteService.list(
                    owner, ProposalRouteSearch.none(), 1).items();
            if (items.isEmpty()) {
                continue;
            }
            // 첫 결재자는 만든 사람 자신이라 반드시 풀린다 — [부서/직급] 이름(사번) 형식.
            assertThat(items.get(0).routeChain()).contains("[").contains("]").contains("(");
            return;
        }
    }

    @Test
    @DisplayName("남의 결재라인은 보이지 않는다")
    void showsOnlyMine() {
        // 결재라인이 없는 사용자에게는 아무것도 나오지 않는다.
        var page = proposalRouteService.list(
                "no-such-user", ProposalRouteSearch.none(), 1);

        assertThat(page.items()).isEmpty();
    }

    @Test
    @DisplayName("등록하면 만든 사람이 결재 순서 맨 앞에 들어간다")
    @Transactional
    void createPutsCreatorFirst() {
        // 실재하는 사번 둘을 결재자로 고른다(FK 안전). admin 이 만든 사람. 롤백된다.
        List<String> pickers = jdbc.queryForList(
                "SELECT id FROM user_view WHERE id <> 'admin' LIMIT 2", String.class);
        assumeTrue(pickers.size() == 2, "사용자가 둘 미만이라 건너뛴다");

        boolean created = proposalRouteService.create("admin", "테스트 라인", pickers);
        assertThat(created).isTrue();

        String route = jdbc.queryForObject(
                "SELECT route FROM proposal_route_tbl ORDER BY id DESC LIMIT 1", String.class);
        // 만든 사람(admin) + 고른 둘 = admin;p1;p2
        assertThat(route).isEqualTo("admin;" + pickers.get(0) + ";" + pickers.get(1));
    }

    @Test
    @DisplayName("수정 화면은 결재관리번호가 없으면 잘못된 접근으로 보낸다")
    void updateWithoutIdRedirects() throws Exception {
        mockMvc.perform(get("/proposal/route-update").with(user(TestUsers.admin())))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .redirectedUrl("/access-error"));
    }

    @Test
    @DisplayName("수정하면 route 가 표 순서 그대로 저장된다(본인 재삽입 없음)")
    @Transactional
    void updateSavesRouteAsIs() {
        // 결재라인을 하나 만들고, 그 id 로 결재자를 바꿔 수정한다. 롤백된다.
        List<String> pickers = jdbc.queryForList(
                "SELECT id FROM user_view WHERE id <> 'admin' LIMIT 2", String.class);
        assumeTrue(pickers.size() == 2, "사용자가 둘 미만이라 건너뛴다");
        proposalRouteService.create("admin", "라인", pickers);
        int id = jdbc.queryForObject(
                "SELECT id FROM proposal_route_tbl ORDER BY id DESC LIMIT 1", Integer.class);

        boolean updated = proposalRouteService.update(id, "바뀐 이름", List.of(pickers.get(1)));

        assertThat(updated).isTrue();
        // 등록과 달리 본인을 앞에 다시 넣지 않는다 — 표에 있던 그대로.
        assertThat(jdbc.queryForObject(
                "SELECT route FROM proposal_route_tbl WHERE id = ?", String.class, id))
                .isEqualTo(pickers.get(1));
    }
}
