package com.erflow.admin.home;

import com.erflow.common.WorkStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.erflow.auth.TestUsers;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 관리자 대시보드가 실제 데이터로 그려지는지 확인한다.
 *
 * <p>이 화면은 «오늘» 에 기댄다 — 그날의 근무 기록을 읽어 표와 그래프를 만든다. 근무
 * 데이터가 2023년치라 오늘 날짜로는 아무것도 안 나오므로, 시계를 그날로 고정해 두고
 * 본다. 고정하지 않으면 «빈 화면이 그려졌다» 만 확인하게 된다.
 */
@SpringBootTest(properties = "server.port=0")
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AdminHomeScreenTest {

    /** 근무 기록이 있는 날. 라이브 데이터에서 고른다. */
    private static final String WORK_DAY = "2023-11-10";

    /** 시계를 그날 정오로 고정한다. */
    @TestConfiguration
    static class FixedClock {

        // 이름을 달리해야 한다. 같은 이름이면 앱의 bean 을 덮어쓰려 해서 기동이 막힌다.
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(
                    Instant.parse(WORK_DAY + "T03:00:00Z"), ZoneId.of("Asia/Seoul"));
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminHomeService homeService;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeAll
    static void requireLocalConfig() {
        assumeTrue(
                new ClassPathResource("application-local.yml").exists(),
                "application-local.yml 이 없어 건너뛴다");
    }

    @Test
    @DisplayName("대시보드가 다섯 칸으로 그려진다")
    void dashboardRenders() throws Exception {
        String html = mockMvc.perform(get("/admin").with(user(TestUsers.admin())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("근무 현황").contains("결재").contains("수/발주 내역")
                .contains("공지사항").contains("출/퇴근관리")
                // 꼬리말은 관리자 화면 중 여기에만 있다.
                .contains("Dong-eui University");
    }

    @Test
    @DisplayName("네 칸이 실제 데이터로 채워진다")
    void dashboardHasData() {
        var dashboard = homeService.dashboard();

        assertThat(dashboard.proposals()).isNotEmpty();
        assertThat(dashboard.tasks()).isNotEmpty();
        assertThat(dashboard.notices()).isNotEmpty();
        assertThat(dashboard.works()).isNotEmpty();
    }

    @Test
    @DisplayName("결재 제목은 10글자에서, 이름은 6글자에서 잘린다")
    void longTextIsTruncated() {
        assertThat(new RecentProposal(1, "admin", "1234567890123", "2023-11-10", 1)
                .shortSubject()).isEqualTo("1234567890...");
        assertThat(new RecentProposal(1, "admin", "12345", "2023-11-10", 1)
                .shortSubject()).isEqualTo("12345");
        assertThat(new WorkRow("일곱글자이름입니다", null, null, 0).shortName())
                .isEqualTo("일곱글자이름...");
    }

    @Test
    @DisplayName("근무 시간은 한 시간을 빼고 찍는다")
    void workedTimeSubtractsAnHour() {
        // 09:00 에 출근해 18:00 에 퇴근하면 8시간이다.
        WorkRow worked = new WorkRow(
                "홍길동", "2023-11-10 09:00:00", "2023-11-10 18:00:00", 2);
        assertThat(worked.workedTime()).isEqualTo("08:00");

        // 시간을 세지 않는 상태는 «-» 다.
        assertThat(new WorkRow("홍길동", "2023-11-10 09:00:00", null, 0).workedTime())
                .isEqualTo("-");
    }

    @Test
    @DisplayName("상태 라벨은 한 벌이다 — 표도 그래프도 같은 이름을 쓴다")
    void statusLabelsAreOneVocabulary() {
        // 레거시는 같은 코드를 두 배열로 읽었고 3·4 가 뒤바뀌어 있었다.
        // 이제 WorkStatus 하나만 있다(D-125).
        assertThat(WorkStatus.label(1)).isEqualTo("근무 중");
        assertThat(WorkStatus.label(3)).isEqualTo("조퇴");
        assertThat(WorkStatus.label(4)).isEqualTo("지각");
        assertThat(WorkStatus.label(9)).isEmpty();
    }

    @Test
    @DisplayName("그래프가 라벨을 붙여 보낸다 — 스크립트가 이름을 갖지 않는다")
    void graphCarriesLabels() throws Exception {
        String body = mockMvc.perform(get("/admin/graph/view").with(user(TestUsers.admin())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // 코드→인원 맵이 아니라 라벨이 들어 있는 목록이다.
        assertThat(body).startsWith("[").contains("\"label\"", "\"status\"", "\"value\"");
    }

    @Test
    @DisplayName("그래프를 심는 자리는 <div> 다 — <svg> 안에 <svg> 를 또 넣지 않는다")
    void graphMountIsAPlainDiv() throws Exception {
        String html = mockMvc.perform(get("/admin").with(user(TestUsers.admin())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // 레거시는 <svg id="graph"> 여기에 d3 가 <svg> 를 또 붙였다(D-069).
        assertThat(html).contains("<div id=\"graph\">");
        assertThat(html).doesNotContain("<svg id=\"graph\"");
    }

    @Test
    @DisplayName("그래프는 그날의 상태별 인원을 코드 순서로 돌려준다")
    void graphCountsByStatus() throws Exception {
        Map<Integer, Integer> counts = homeService.workCounts();

        assertThat(counts).isNotEmpty();
        assertThat(List.copyOf(counts.keySet())).isSorted();
        // DB 에서 직접 센 것과 같아야 한다.
        List<Map<String, Object>> expected = jdbc.queryForList(
                "SELECT status, COUNT(*) AS c FROM work_view "
                        + "WHERE started_at LIKE ? OR ended_at LIKE ? GROUP BY status",
                "%" + WORK_DAY + "%", "%" + WORK_DAY + "%");
        for (Map<String, Object> row : expected) {
            int status = ((Number) row.get("status")).intValue();
            int count = ((Number) row.get("c")).intValue();
            assertThat(counts).containsEntry(status, count);
        }

        mockMvc.perform(get("/admin/graph/view").with(user(TestUsers.admin())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자가 아니면 대시보드도 그래프도 볼 수 없다")
    void nonAdminIsBlocked() throws Exception {
        mockMvc.perform(get("/admin").with(user(TestUsers.noPermission())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/graph/view").with(user(TestUsers.noPermission())))
                .andExpect(status().isForbidden());
    }
}
