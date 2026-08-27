package com.erflow.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.erflow.auth.AuthUser;
import com.erflow.auth.ErflowUserDetails;
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
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 프로필 화면이 실제 데이터로 도는지 확인한다.
 *
 * <p>이 도메인에서 게이트가 못 보는 자리 — 남의 근무 기록은 조회조차 하지 않는 것,
 * 수정으로 들어가는 문 — 을 여기서 못 박는다. 전화번호가 지워지던 결함(D-080)은
 * D-100 으로, 확인이 문이 아니던 결함(D-079)은 D-111 로 2단계에서 고쳤고, 고친
 * 동작을 시험이 지킨다.
 */
@SpringBootTest(properties = "server.port=0")
@AutoConfigureMockMvc
@ActiveProfiles("local")
class ProfileScreenTest {

    /** 근무 기록이 있는 달. 라이브 데이터에서 고른다. */
    private static final YearMonth WORK_MONTH = YearMonth.of(2023, 11);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProfileService profileService;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeAll
    static void requireLocalConfig() {
        assumeTrue(
                new ClassPathResource("application-local.yml").exists(),
                "application-local.yml 이 없어 건너뛴다");
    }

    @Test
    @DisplayName("프로필이 그려진다")
    void viewRenders() throws Exception {
        String html = mockMvc.perform(get("/profile").param("id", "admin")
                        .with(user(TestUsers.admin())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("이번 달 근무 현황")
                .contains("내선 전화").contains("개인 전화")
                .contains("꺾이지 않는 마음!");

        // 부트스트랩 견본에서 온 다섯 줄(Website·Github·Twitter·Instagram·Facebook,
        // 값은 전부 bootdey)을 지웠다(D-122). 1단계는 «화면에 보이니 옮긴다» 로
        // 두었지만 이제 화면은 우리가 정한다 — 남의 견본 글자가 사용자 프로필에
        // 있을 이유가 없다.
        assertThat(html).as("견본 잔재").doesNotContain("bootdey");
    }

    @Test
    @DisplayName("자기 프로필에만 Edit 이 뜬다")
    void editOnlyOnOwnProfile() throws Exception {
        String other = someoneElse();

        assertThat(profileHtml("admin", "admin")).contains("Edit");
        assertThat(profileHtml("admin", other)).doesNotContain("Edit");
    }

    @Test
    @DisplayName("없는 사번이면 잘못된 접근으로 보낸다")
    void unknownIdIsRejected() throws Exception {
        mockMvc.perform(get("/profile").param("id", "no-such-user")
                        .with(user(TestUsers.admin())))
                .andExpect(redirectedUrl("/access-error"));
        // id 를 아예 주지 않아도 마찬가지다.
        mockMvc.perform(get("/profile").with(user(TestUsers.admin())))
                .andExpect(redirectedUrl("/access-error"));
    }

    @Test
    @DisplayName("읽을 수 없는 달이면 잘못된 접근으로 보낸다")
    void unreadableMonthIsRejected() throws Exception {
        mockMvc.perform(get("/profile").param("id", "admin").param("date", "언제")
                        .with(user(TestUsers.admin())))
                .andExpect(redirectedUrl("/access-error"));
        // 빈 값은 «이번 달» 이라는 뜻이다.
        mockMvc.perform(get("/profile").param("id", "admin").param("date", "")
                        .with(user(TestUsers.admin())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("남의 프로필에서는 근무 기록을 아예 읽지 않는다")
    void othersWorkIsNeverLoaded() {
        String owner = workerWithRecords();

        WorkCalendar mine = profileService.calendar(owner, owner, WORK_MONTH);
        WorkCalendar theirs = profileService.calendar("admin", owner, WORK_MONTH);

        assertThat(coloured(mine)).isPositive();
        assertThat(coloured(theirs)).isZero();
    }

    @Test
    @DisplayName("달 고르기의 하한은 언제나 올해 1월이다")
    void lowerBoundIsJanuaryOfThisYear() {
        assertThat(profileService.minMonth().getMonthValue()).isEqualTo(1);
        assertThat(profileService.minMonth().getYear())
                .isEqualTo(profileService.thisMonth().getYear());
    }

    @Test
    @DisplayName("주민 번호는 화면에 오르지 않는다 — 읽지도 않는다")
    void socialNumberNeverReachesTheScreen() throws Exception {
        String stored = jdbc.queryForObject(
                "SELECT social_number FROM user_tbl WHERE id = 'admin'", String.class);
        assumeTrue(stored != null && !stored.isBlank(), "주민번호가 비어 있어 건너뛴다");

        mockMvc.perform(get("/profile/update").sessionAttr("profilePasswordChecked", true)
                        .with(user(TestUsers.admin())))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString(stored))));
    }

    @Test
    @DisplayName("비밀번호 확인이 진짜 문이 됐다 — D-079 를 2단계에서 고쳤다(D-111)")
    void updateFormRequiresThePasswordCheckNow() throws Exception {
        // 레거시는 로그인만 보고 열어, 주소를 직접 치면 확인 없이 들어갔다(D-079).
        mockMvc.perform(get("/profile/update").with(user(TestUsers.admin())))
                .andExpect(redirectedUrl("/profile/password-check"));

        // 확인을 통과한 세션(표가 있다)만 들어온다.
        mockMvc.perform(get("/profile/update").sessionAttr("profilePasswordChecked", true)
                        .with(user(TestUsers.admin())))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("사원 수정")));

        // 처리도 같은 문이다 — 확인 없이 바로 POST 해도 막힌다.
        mockMvc.perform(post("/profile/update-proc")
                        .param("name", "x").param("email", "").param("postalCode", "")
                        .param("address1", "").param("address2", "").param("mobilePhone", "")
                        .with(user(TestUsers.admin()))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(redirectedUrl("/profile/password-check"));
    }

    @Test
    @DisplayName("비밀번호가 틀리면 알린 뒤 프로필로 되돌린다")
    void wrongPasswordGoesBack() throws Exception {
        String html = mockMvc.perform(post("/profile/password-check-proc")
                        .param("password", "이건아닐것이다")
                        .with(user(TestUsers.admin()))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("비밀번호가 틀렸습니다.").contains("/profile?id=admin");
    }

    @Test
    @DisplayName("비밀번호를 아예 보내지 않으면 잘못된 접근이다")
    void missingPasswordIsRejected() throws Exception {
        mockMvc.perform(post("/profile/password-check-proc")
                        .with(user(TestUsers.admin()))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(redirectedUrl("/access-error"));
    }

    @Test
    @DisplayName("수정 화면의 개인 번호 칸에 현재 값이 채워진다 — 저장해도 안 지워진다(D-100)")
    @Transactional
    void phoneNumberSurvivesASave() throws Exception {
        jdbc.update("UPDATE user_tbl SET mobile_phone = '010-1111-2222' WHERE id = 'admin'");

        // 레거시는 이 칸을 비워 뒀고(D-080) 저장할 때마다 전화번호가 지워졌다.
        // 2단계에서 현재 값을 채웠다 — 화면이 채워 주므로 그대로 저장하면 값이 산다.
        String html = mockMvc.perform(get("/profile/update")
                        .sessionAttr("profilePasswordChecked", true)
                        .with(user(TestUsers.admin())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("010-1111-2222");

        ProfileUser before = profileService.get("admin");
        profileService.update(new ProfileUser("admin", before.name(), before.email(),
                null, before.mobilePhone(), before.postalCode(),
                before.address1(), before.address2()));

        assertThat(profileService.get("admin").mobilePhone()).isEqualTo("010-1111-2222");
    }

    @Test
    @DisplayName("이름은 공백만 넣으면 비어 버리고, 우편번호는 공백이 그대로 남는다")
    @Transactional
    void onlySomeFieldsAreTrimmed() throws Exception {
        mockMvc.perform(post("/profile/update-proc")
                        .param("name", "  고친이름  ")
                        .param("email", "   ")
                        .param("postalCode", "   ")
                        .param("address1", "길 1")
                        .param("address2", "2층")
                        .param("mobilePhone", "")
                        .sessionAttr("profilePasswordChecked", true)
                        .with(user(TestUsers.admin()))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk());

        ProfileUser saved = profileService.get("admin");
        assertThat(saved.name()).isEqualTo("고친이름");
        // 다듬어진 칸은 빈 값이 null 이 된다.
        assertThat(saved.email()).isNull();
        // 우편번호는 다듬지 않은 원본이 그대로 들어간다.
        assertThat(saved.postalCode()).isEqualTo("   ");
    }

    @Test
    @DisplayName("칸이 하나라도 오지 않으면 잘못된 접근이다")
    void missingFieldIsRejected() throws Exception {
        mockMvc.perform(post("/profile/update-proc")
                        .param("name", "이름")
                        .param("email", "a@b.c")
                        .param("postalCode", "12345")
                        .param("address1", "길 1")
                        .param("address2", "2층")
                        .sessionAttr("profilePasswordChecked", true)
                        .with(user(TestUsers.admin()))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(redirectedUrl("/access-error"));
    }

    @Test
    @DisplayName("주소는 둘을 공백으로 잇는다 — 둘 다 비면 공백 한 글자다")
    void addressIsAlwaysJoinedByASpace() {
        assertThat(new ProfileUser("a", "이름", null, null, null, null, "길 1", "2층")
                .addressText()).isEqualTo("길 1 2층");
        assertThat(new ProfileUser("a", "이름", null, null, null, null, null, null)
                .addressText()).isEqualTo(" ");
    }

    @Test
    @DisplayName("권한이 없으면 프로필을 볼 수 없다")
    void withoutPermissionBlocked() throws Exception {
        mockMvc.perform(get("/profile").param("id", "admin")
                        .with(user(TestUsers.noPermission())))
                .andExpect(status().isForbidden());
    }

    private String profileHtml(String viewer, String owner) throws Exception {
        return mockMvc.perform(get("/profile").param("id", owner)
                        .with(user(new ErflowUserDetails(
                                new AuthUser(viewer, "시험", "(시험용)",
                                        Long.MIN_VALUE, Long.MIN_VALUE), false))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private String someoneElse() {
        return jdbc.queryForObject(
                "SELECT id FROM user_tbl WHERE id != 'admin' LIMIT 1", String.class);
    }

    private String workerWithRecords() {
        String owner = jdbc.queryForObject(
                "SELECT user_id FROM work_view WHERE started_at LIKE '2023-11%' LIMIT 1",
                String.class);
        assumeTrue(owner != null, "그 달의 근무 기록이 없어 건너뛴다");
        return owner;
    }

    private static int coloured(WorkCalendar calendar) {
        return (int) java.util.stream.Stream.concat(
                        calendar.firstHalf().days().stream(),
                        calendar.secondHalf().days().stream())
                .filter(day -> day.cellClass() != null && day.cellClass().contains(" is-"))
                .count();
    }
}
