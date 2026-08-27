package com.erflow.process;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
 * 공정 화면이 실제 DB 를 상대로 도는지 확인한다.
 *
 * <p><b>공정 데이터가 하나도 없다.</b> 레거시 DB 도 비어 있어 «있는 것을 그대로 보여주는가»
 * 는 확인할 수 없다. 그래서 넣고 확인하고 되돌리는 방식으로 본다 — 특히 앞뒤 고리가
 * 제대로 이어지는지가 이 도메인의 전부다.
 */
@SpringBootTest(properties = "server.port=0")
@AutoConfigureMockMvc
@ActiveProfiles("local")
class ProcessScreenTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProcessService processService;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeAll
    static void requireLocalConfig() {
        assumeTrue(
                new ClassPathResource("application-local.yml").exists(),
                "application-local.yml 이 없어 건너뛴다");
    }

    @Test
    @DisplayName("공정 목록이 그려진다")
    void listRenders() throws Exception {
        String html = mockMvc.perform(get("/process/list").with(user(TestUsers.admin())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("사용자 &gt; 생산관리 &gt; 공정 관리")
                .contains("이전 공정ID").contains("다음 공정ID").contains("우선순위");
    }

    @Test
    @DisplayName("등록 화면과 이름 변경 창이 그려진다")
    void formsRender() throws Exception {
        assertThat(mockMvc.perform(get("/process/register").with(user(TestUsers.admin())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString())
                .contains("공정 추가").contains("공정 등록");

        // 이름 변경 창은 DB 를 읽지 않고 주소에 실린 값을 그대로 채운다.
        assertThat(mockMvc.perform(get("/process/update")
                        .param("id", "P-1").param("name", "절단")
                        .with(user(TestUsers.admin())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString())
                .contains("value=\"P-1\"").contains("value=\"절단\"");
    }

    @Test
    @DisplayName("등록하면 앞뒤 고리가 이어지고 우선순위가 1부터 붙는다")
    @Transactional
    void createChainLinksNeighbours() {
        assertThat(processService.createChain(List.of(
                new ProcessService.ProcessStep("T-1", "절단"),
                new ProcessService.ProcessStep("T-2", "가공"),
                new ProcessService.ProcessStep("T-3", "포장")))).isTrue();

        var rows = jdbc.queryForList(
                "SELECT id, process_tbl_prev_id AS prev, process_tbl_next_id AS next, priority "
                        + "FROM process_tbl WHERE id LIKE 'T-%' ORDER BY priority");
        assertThat(rows).hasSize(3);
        // 첫 공정은 이전이 없고 다음만 있다.
        assertThat(rows.get(0).get("prev")).isNull();
        assertThat(rows.get(0).get("next")).isEqualTo("T-2");
        assertThat(((Number) rows.get(0).get("priority")).intValue()).isEqualTo(1);
        // 가운데는 양쪽 다 있다.
        assertThat(rows.get(1).get("prev")).isEqualTo("T-1");
        assertThat(rows.get(1).get("next")).isEqualTo("T-3");
        // 마지막은 다음이 없다.
        assertThat(rows.get(2).get("prev")).isEqualTo("T-2");
        assertThat(rows.get(2).get("next")).isNull();
        assertThat(((Number) rows.get(2).get("priority")).intValue()).isEqualTo(3);
    }

    @Test
    @DisplayName("공정이 하나뿐이면 등록되지 않는다 — 레거시가 그 자리에서 죽는다")
    @Transactional
    void singleProcessCannotBeRegistered() {
        assertThat(processService.createChain(
                List.of(new ProcessService.ProcessStep("T-9", "혼자")))).isFalse();

        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM process_tbl WHERE id = 'T-9'", Integer.class)).isZero();
    }

    @Test
    @DisplayName("이름만 고친다 — 고리와 우선순위는 그대로다")
    @Transactional
    void renameKeepsChain() {
        processService.createChain(List.of(
                new ProcessService.ProcessStep("T-1", "절단"),
                new ProcessService.ProcessStep("T-2", "가공")));

        assertThat(processService.rename("T-1", "새이름")).isTrue();

        var row = jdbc.queryForMap(
                "SELECT name, process_tbl_next_id AS next, priority FROM process_tbl "
                        + "WHERE id = 'T-1'");
        assertThat(row.get("name")).isEqualTo("새이름");
        assertThat(row.get("next")).isEqualTo("T-2");
        assertThat(((Number) row.get("priority")).intValue()).isEqualTo(1);
    }

    @Test
    @DisplayName("없는 공정은 고칠 수 없다")
    void renameMissingFails() {
        assertThat(processService.rename("없는공정", "이름")).isFalse();
    }

    @Test
    @DisplayName("가운데를 지우면 앞뒤가 이어지고 자리 번호가 당겨진다 (D-133)")
    @Transactional
    void deleteRelinksChain() {
        processService.createChain(List.of(
                new ProcessService.ProcessStep("T-1", "절단"),
                new ProcessService.ProcessStep("T-2", "가공"),
                new ProcessService.ProcessStep("T-3", "도장")));

        assertThat(processService.delete(List.of("T-2"))).isTrue();

        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM process_tbl WHERE id = 'T-2'", Integer.class)).isZero();
        var first = jdbc.queryForMap(
                "SELECT process_tbl_next_id AS next, priority FROM process_tbl WHERE id = 'T-1'");
        assertThat(first.get("next")).isEqualTo("T-3");
        assertThat(((Number) first.get("priority")).intValue()).isEqualTo(1);
        var last = jdbc.queryForMap(
                "SELECT process_tbl_prev_id AS prev, priority FROM process_tbl WHERE id = 'T-3'");
        assertThat(last.get("prev")).isEqualTo("T-1");
        assertThat(((Number) last.get("priority")).intValue()).isEqualTo(2);
    }

    @Test
    @DisplayName("첫 공정을 지우면 다음 공정이 머리가 된다 (D-133)")
    @Transactional
    void deleteHeadPromotesNext() {
        processService.createChain(List.of(
                new ProcessService.ProcessStep("T-1", "절단"),
                new ProcessService.ProcessStep("T-2", "가공")));

        assertThat(processService.delete(List.of("T-1"))).isTrue();

        var row = jdbc.queryForMap(
                "SELECT process_tbl_prev_id AS prev, process_tbl_next_id AS next, priority "
                        + "FROM process_tbl WHERE id = 'T-2'");
        assertThat(row.get("prev")).isNull();
        assertThat(row.get("next")).isNull();
        assertThat(((Number) row.get("priority")).intValue()).isEqualTo(1);
    }

    @Test
    @DisplayName("참조되는 공정은 지우지 않는다 — 하나라도 걸리면 전부 그대로 (D-133)")
    @Transactional
    void deleteRefusesReferencedProcess() {
        processService.createChain(List.of(
                new ProcessService.ProcessStep("T-1", "절단"),
                new ProcessService.ProcessStep("T-2", "가공")));
        jdbc.update("INSERT INTO production_tbl (process_tbl_id, product_tbl_id, count) "
                + "VALUES ('T-2', 'P-검증', 1)");

        assertThat(processService.delete(List.of("T-1", "T-2"))).isFalse();

        // T-1 은 지울 수 있었지만 T-2 가 걸렸으므로 둘 다 남아야 한다.
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM process_tbl WHERE id IN ('T-1', 'T-2')", Integer.class))
                .isEqualTo(2);
    }

    @Test
    @DisplayName("없는 공정을 지우면 실패다")
    void deleteMissingFails() {
        assertThat(processService.delete(List.of("없는공정"))).isFalse();
    }

    @Test
    @DisplayName("검색은 칸마다 조건이 다르다 — 우선순위만 정확히 같은 값")
    @Transactional
    void searchDiffersByField() {
        processService.createChain(List.of(
                new ProcessService.ProcessStep("T-1", "절단"),
                new ProcessService.ProcessStep("T-2", "가공")));

        assertThat(processService.list(new ProcessSearch("id", "T-"), 1).rows()).hasSize(2);
        assertThat(processService.list(new ProcessSearch("name", "절"), 1).rows()).hasSize(1);
        assertThat(processService.list(new ProcessSearch("priority", "1"), 1).rows()).hasSize(1);
        // 숫자가 아니면 조건이 붙지 않는다(레거시는 그 자리에서 죽는다).
        assertThat(processService.list(new ProcessSearch("priority", "하나"), 1).rows())
                .hasSize(2);
    }

    @Test
    @DisplayName("권한이 없으면 목록을 볼 수 없다")
    void withoutPermissionBlocked() throws Exception {
        mockMvc.perform(get("/process/list").with(user(TestUsers.noPermission())))
                .andExpect(status().isForbidden());
    }
}
