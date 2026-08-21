package com.erflow.calendar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.erflow.auth.TestUsers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 메인 화면 달력이 부르는 창구.
 *
 * <p>여기는 화면이 아니라 JSON 이라 정합성 게이트가 아예 닿지 않는다. 공개 범위별
 * 권한, 남의 일정을 고칠 수 있는 구멍(D-083), 그리고 CSRF 토큰이 <b>정말 필요한지</b>
 * 를 시험으로 못 박는다.
 */
@SpringBootTest(properties = "server.port=0")
@AutoConfigureMockMvc
@ActiveProfiles("local")
class CalendarApiTest {

    private static final String PERSONAL = """
            {"userId":"admin","subject":"시험일정","content":"본문",
             "start":"2026-08-20 09:00:00","end":"2026-08-20 18:00:00","type":0}""";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CalendarService calendarService;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeAll
    static void requireLocalConfig() {
        assumeTrue(
                new ClassPathResource("application-local.yml").exists(),
                "application-local.yml 이 없어 건너뛴다");
    }

    @Test
    @DisplayName("일정 목록이 JSON 으로 나간다")
    void viewReturnsJson() throws Exception {
        mockMvc.perform(get("/calendar/view").with(user(TestUsers.admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("CSRF 토큰이 없으면 막힌다 — 그래서 csrf.js 가 필요하다")
    void jsonPostWithoutTokenIsBlocked() throws Exception {
        mockMvc.perform(post("/calendar/insert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PERSONAL)
                        .with(user(TestUsers.admin())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("토큰을 실으면 통과한다")
    @Transactional
    void jsonPostWithTokenPasses() throws Exception {
        mockMvc.perform(post("/calendar/insert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PERSONAL)
                        .with(user(TestUsers.admin()))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    @DisplayName("남의 사번으로 보내면 아무 일도 일어나지 않는다")
    @Transactional
    void cannotActAsSomeoneElse() throws Exception {
        String body = PERSONAL.replace("\"admin\"", "\"someone-else\"");

        mockMvc.perform(post("/calendar/insert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(user(TestUsers.admin()))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("error"));

        assertThat(count("시험일정")).isZero();
    }

    @Test
    @DisplayName("개인일정은 누구나, 전체일정은 관리자만 만든다")
    void scopeDecidesWhoMayWrite() {
        assertThat(calendarService.allowed(TestUsers.noPermission(), 0)).isTrue();
        assertThat(calendarService.allowed(TestUsers.noPermission(), 1)).isFalse();
        assertThat(calendarService.allowed(TestUsers.noPermission(), 2)).isFalse();
        assertThat(calendarService.allowed(TestUsers.admin(), 2)).isTrue();
        // 레거시 switch 에 없는 값은 어느 갈래에도 걸리지 않는다.
        assertThat(calendarService.allowed(TestUsers.admin(), 9)).isFalse();
    }

    @Test
    @DisplayName("부서일정은 «부서 일정 등록» 프로그램 권한을 따른다")
    void deptScopeFollowsTheProgram() {
        Integer rows = jdbc.queryForObject(
                "SELECT COUNT(*) FROM permission_program_tbl WHERE program_id = ?",
                Integer.class, CalendarService.DEPT_CALENDAR_PROGRAM);
        assumeTrue(rows != null && rows > 0, "그 프로그램의 권한 행이 없어 건너뛴다");

        // 권한이 0 인 사람은 어떤 값이 걸려 있어도 통과하지 못한다.
        assertThat(calendarService.allowed(TestUsers.noPermission(), 1)).isFalse();
    }

    @Test
    @DisplayName("읽을 수 없는 종료 시각은 «없음» 이 된다 — 거절이 아니다")
    @Transactional
    void unreadableEndBecomesNull() throws Exception {
        String body = PERSONAL.replace("2026-08-20 18:00:00", "언제까지인지모름");

        mockMvc.perform(post("/calendar/insert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(user(TestUsers.admin()))
                        .with(csrf()))
                .andExpect(jsonPath("$.status").value("success"));

        assertThat(jdbc.queryForObject(
                "SELECT ended_at FROM calendar_tbl WHERE subject = '시험일정'", String.class))
                .isNull();
    }

    @Test
    @DisplayName("남의 일정은 고칠 수 없다 — 레거시의 구멍을 2단계에서 막았다(D-101)")
    @Transactional
    void updateChecksTheOwnerNow() {
        String other = jdbc.queryForObject(
                "SELECT id FROM user_tbl WHERE id != 'admin' LIMIT 1", String.class);
        jdbc.update("INSERT INTO calendar_tbl "
                + "(user_tbl_id, subject, content, started_at, ended_at, type) "
                + "VALUES (?, '남의일정', '본문', '2026-08-20 09:00:00', "
                + "'2026-08-20 18:00:00', 0)", other);
        int id = jdbc.queryForObject(
                "SELECT id FROM calendar_tbl WHERE subject = '남의일정'", Integer.class);

        // 레거시는 번호만 맞으면 남의 일정도 고쳐졌다(D-083). 이제 아무 일도 없다.
        boolean done = calendarService.update(TestUsers.admin(), new CalendarEvent(
                id, "admin", 0, null, "가로챈일정", "본문",
                "2026-08-20 09:00:00", "2026-08-20 18:00:00", 0));

        assertThat(done).isFalse();
        assertThat(jdbc.queryForObject(
                "SELECT subject FROM calendar_tbl WHERE id = ?", String.class, id))
                .isEqualTo("남의일정");

        // 자기 일정은 고쳐진다.
        jdbc.update("INSERT INTO calendar_tbl "
                + "(user_tbl_id, subject, content, started_at, ended_at, type) "
                + "VALUES ('admin', '내일정', '본문', '2026-08-20 09:00:00', "
                + "'2026-08-20 18:00:00', 0)");
        int mine = jdbc.queryForObject(
                "SELECT id FROM calendar_tbl WHERE subject = '내일정'", Integer.class);
        assertThat(calendarService.update(TestUsers.admin(), new CalendarEvent(
                mine, "admin", 0, null, "고친일정", "본문",
                "2026-08-20 09:00:00", "2026-08-20 18:00:00", 0))).isTrue();
    }

    @Test
    @DisplayName("일정 지우기는 주인을 본다 — 레거시부터 그랬다")
    @Transactional
    void deleteChecksTheOwner() {
        String other = jdbc.queryForObject(
                "SELECT id FROM user_tbl WHERE id != 'admin' LIMIT 1", String.class);
        jdbc.update("INSERT INTO calendar_tbl "
                + "(user_tbl_id, subject, content, started_at, ended_at, type) "
                + "VALUES (?, '남의일정', '본문', '2026-08-20 09:00:00', "
                + "'2026-08-20 18:00:00', 0)", other);
        int id = jdbc.queryForObject(
                "SELECT id FROM calendar_tbl WHERE subject = '남의일정'", Integer.class);

        assertThat(calendarService.delete("admin", id)).isFalse();
        assertThat(count("남의일정")).isEqualTo(1);
        assertThat(calendarService.delete(other, id)).isTrue();
        assertThat(count("남의일정")).isZero();
    }

    @Test
    @DisplayName("개인일정은 만든 사람에게만 보인다")
    @Transactional
    void personalEventIsPrivate() {
        String other = jdbc.queryForObject(
                "SELECT id FROM user_tbl WHERE id != 'admin' LIMIT 1", String.class);
        jdbc.update("INSERT INTO calendar_tbl "
                + "(user_tbl_id, subject, content, started_at, ended_at, type) "
                + "VALUES ('admin', '내일정', '본문', '2026-08-20 09:00:00', "
                + "'2026-08-20 18:00:00', 0)");

        assertThat(calendarService.visible("admin"))
                .anySatisfy(event -> assertThat(event.subject()).isEqualTo("내일정"));
        assertThat(calendarService.visible(other))
                .noneSatisfy(event -> assertThat(event.subject()).isEqualTo("내일정"));
    }

    @Test
    @DisplayName("전체일정은 누구에게나 보인다")
    @Transactional
    void companyWideEventIsVisibleToAll() {
        String other = jdbc.queryForObject(
                "SELECT id FROM user_tbl WHERE id != 'admin' LIMIT 1", String.class);
        jdbc.update("INSERT INTO calendar_tbl "
                + "(user_tbl_id, subject, content, started_at, ended_at, type) "
                + "VALUES ('admin', '전체일정', '본문', '2026-08-20 09:00:00', "
                + "'2026-08-20 18:00:00', 2)");

        assertThat(calendarService.visible(other))
                .anySatisfy(event -> assertThat(event.subject()).isEqualTo("전체일정"));
    }

    private int count(String subject) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM calendar_tbl WHERE subject = ?", Integer.class, subject);
    }
}
