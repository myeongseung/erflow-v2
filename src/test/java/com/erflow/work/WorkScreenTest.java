package com.erflow.work;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.erflow.auth.TestUsers;
import java.time.YearMonth;
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

/**
 * 근태 확인이 실제 데이터로 도는지 확인한다.
 *
 * <p>게이트가 못 보는 자리 — 8명씩 끊는 페이징, 부서/이름 검색의 갈림, admin 이
 * 목록에서 빠지는 것 — 을 여기서 못 박는다. 셈 자체는 {@code AttendanceSheetTest} 가
 * DB 없이 본다.
 */
@SpringBootTest(properties = "server.port=0")
@AutoConfigureMockMvc
@ActiveProfiles("local")
class WorkScreenTest {

    /** 근무 기록이 있는 달. 라이브 데이터에서 고른다. */
    private static final YearMonth WORK_MONTH = YearMonth.of(2023, 11);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WorkService workService;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeAll
    static void requireLocalConfig() {
        assumeTrue(
                new ClassPathResource("application-local.yml").exists(),
                "application-local.yml 이 없어 건너뛴다");
    }

    @Test
    @DisplayName("근태 확인이 그려진다")
    void screenRenders() throws Exception {
        String html = mockMvc.perform(get("/work").with(user(TestUsers.admin())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("사용자 &gt; 근태 관리 &gt; 근태 확인")
                .contains("성명").contains("부서").contains("직책")
                .contains("정상").contains("지각").contains("조퇴").contains("연차");
    }

    @Test
    @DisplayName("읽을 수 없는 달이면 잘못된 접근으로 보낸다")
    void unreadableMonthIsRejected() throws Exception {
        mockMvc.perform(get("/work").param("date", "언제")
                        .with(user(TestUsers.admin())))
                .andExpect(redirectedUrl("/access-error"));
        mockMvc.perform(get("/work").param("date", "")
                        .with(user(TestUsers.admin())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("8명씩 끊는다 — 한 사람이 두 줄을 차지한다")
    void eightUsersPerPage() {
        WorkService.WorkPage page = workService.page(WORK_MONTH, "", "", 1);

        assertThat(page.rows()).hasSizeLessThanOrEqualTo(8);
        assertThat(page.pagination().numPerPage()).isEqualTo(8);
    }

    @Test
    @DisplayName("admin 은 목록에서 빠진다")
    void adminIsExcluded() {
        WorkService.WorkPage page = workService.page(WORK_MONTH, "", "", 1);

        assertThat(page.rows())
                .noneSatisfy(row -> assertThat(row.user().id()).isEqualTo("admin"));
    }

    @Test
    @DisplayName("부서 검색과 이름 검색이 따로 논다")
    void searchSplitsByField() {
        String dept = jdbc.queryForObject(
                "SELECT dept_name FROM user_view WHERE id != 'admin' LIMIT 1", String.class);
        assumeTrue(dept != null, "부서가 없어 건너뛴다");

        WorkService.WorkPage byDept = workService.page(WORK_MONTH, dept, "", 1);
        WorkService.WorkPage byWrongName = workService.page(WORK_MONTH, "", dept, 1);

        assertThat(byDept.rows()).isNotEmpty();
        assertThat(byDept.rows())
                .allSatisfy(row -> assertThat(row.user().deptName()).contains(dept));
        // 부서 이름을 «이름» 칸으로 찾으면 아무도 안 걸린다.
        assertThat(byWrongName.rows()).isEmpty();
    }

    @Test
    @DisplayName("근무 기록이 있는 사람의 칸에 글자가 찍힌다")
    void cellsCarryText() {
        String worker = jdbc.queryForObject(
                "SELECT user_id FROM work_view WHERE started_at LIKE '2023-11%' LIMIT 1",
                String.class);
        assumeTrue(worker != null, "그 달의 근무 기록이 없어 건너뛴다");
        String name = jdbc.queryForObject(
                "SELECT name FROM user_view WHERE id = ?", String.class, worker);

        WorkService.WorkPage page = workService.page(WORK_MONTH, "", name, 1);
        assumeTrue(!page.rows().isEmpty(), "이름 검색이 비어 건너뛴다");

        AttendanceSheet sheet = page.rows().get(0).sheet();
        long filled = sheet.first().cells().stream().filter(c -> !c.text().isEmpty()).count()
                + sheet.second().cells().stream().filter(c -> !c.text().isEmpty()).count();
        assertThat(filled).isPositive();
    }

    @Test
    @DisplayName("달 고르기의 하한은 올해 1월이다")
    void lowerBoundIsJanuary() {
        assertThat(workService.minMonth().getMonthValue()).isEqualTo(1);
        assertThat(workService.minMonth().getYear())
                .isEqualTo(workService.thisMonth().getYear());
    }

    @Test
    @DisplayName("권한이 없으면 볼 수 없다")
    void withoutPermissionBlocked() throws Exception {
        mockMvc.perform(get("/work").with(user(TestUsers.noPermission())))
                .andExpect(status().isForbidden());
    }
}
