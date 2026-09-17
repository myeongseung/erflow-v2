package com.erflow.admin.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.erflow.auth.ErflowUserDetailsService;
import com.erflow.auth.TestUsers;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 비밀번호 초기화 (D-134, O-007 해소 장치).
 *
 * <p>비밀번호=사번인 계정은 사번만 알면 남이 먼저 로그인해 비밀번호를 바꿔 버릴 수
 * 있다. 관리자가 임시 비밀번호를 걸어 그 문을 닫는 기능이므로, <b>사번 로그인이 실제로
 * 막히는지</b>와 <b>«변경 필요» 상태가 걸리고 풀리는지</b>를 실제 DB 로 확인한다.
 */
@SpringBootTest(properties = "server.port=0")
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AdminPasswordResetTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminUserService adminUserService;

    @Autowired
    private ErflowUserDetailsService userDetailsService;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeAll
    static void requireLocalConfig() {
        assumeTrue(
                new ClassPathResource("application-local.yml").exists(),
                "application-local.yml 이 없어 건너뛴다");
    }

    @Test
    @DisplayName("초기화하면 사번으로 로그인할 수 없고 «변경 필요» 상태가 된다")
    @Transactional
    void resetBlocksIdLogin() {
        adminUserService.register(newUser("T-9101"));
        // 등록 직후에는 비밀번호가 사번이다 — O-007 이 말하는 그 상태다.
        assertThat(passwordEncoder.matches("T-9101", storedPassword("T-9101"))).isTrue();

        AdminUserService.TempPassword reset = adminUserService.resetPassword("T-9101");

        assertThat(reset).isNotNull();
        assertThat(reset.id()).isEqualTo("T-9101");
        assertThat(reset.name()).isEqualTo("시험사원");
        // 사번은 더 이상 비밀번호가 아니고, 임시 비밀번호만 통한다.
        String stored = storedPassword("T-9101");
        assertThat(passwordEncoder.matches("T-9101", stored)).isFalse();
        assertThat(passwordEncoder.matches(reset.password(), stored)).isTrue();
        // «변경 필요» 플래그가 켜진다 — 임시 비밀번호는 사번이 아니라서, 이 플래그가
        // 없으면 로그인 직후 변경 강제가 걸리지 않는다.
        assertThat(changeRequired("T-9101")).isEqualTo(1);
    }

    @Test
    @DisplayName("초기화된 계정은 로그인 직후 비밀번호 변경만 할 수 있다")
    @Transactional
    void resetAccountIsForcedToChangePassword() {
        adminUserService.register(newUser("T-9102"));
        adminUserService.resetPassword("T-9102");

        var details = userDetailsService.loadUserByUsername("T-9102");

        assertThat(details.passwordChangeRequired()).isTrue();
        assertThat(details.getAuthorities())
                .extracting(a -> a.getAuthority())
                .containsExactly("ROLE_PASSWORD_CHANGE");
    }

    @Test
    @DisplayName("본인이 새 비밀번호를 정하면 «변경 필요» 상태가 풀린다")
    @Transactional
    void changingPasswordClearsTheFlag() throws Exception {
        adminUserService.register(newUser("T-9103"));
        adminUserService.resetPassword("T-9103");
        var details = userDetailsService.loadUserByUsername("T-9103");

        mockMvc.perform(post("/login/change-password")
                        .param("password", "새비밀번호-9103")
                        .param("rePassword", "새비밀번호-9103")
                        .with(user(details)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login/password-ok"));

        assertThat(changeRequired("T-9103")).isEqualTo(0);
        assertThat(passwordEncoder.matches("새비밀번호-9103", storedPassword("T-9103"))).isTrue();
        // 다시 읽으면 정상 사용자다 — 변경 화면에 갇히지 않는다.
        assertThat(userDetailsService.loadUserByUsername("T-9103")
                .passwordChangeRequired()).isFalse();
    }

    @Test
    @DisplayName("처리 결과 화면이 임시 비밀번호를 보여 준다")
    @Transactional
    void resultPageShowsTempPasswordOnce() throws Exception {
        adminUserService.register(newUser("T-9104"));

        String html = mockMvc.perform(post("/admin/user/reset-password")
                        .param("id", "T-9104")
                        .with(user(TestUsers.admin())).with(csrf()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("비밀번호 초기화")
                .contains("시험사원(T-9104)")
                .contains("다시 볼 수 없습니다");
        // 화면에 실린 값이 실제로 걸린 임시 비밀번호다.
        Matcher matcher = Pattern.compile("<strong[^>]*>([^<]+)</strong>").matcher(html);
        assertThat(matcher.find()).isTrue();
        assertThat(passwordEncoder.matches(matcher.group(1), storedPassword("T-9104"))).isTrue();
    }

    @Test
    @DisplayName("없는 사번이면 잘못된 접근으로 보낸다")
    void unknownIdRedirects() throws Exception {
        mockMvc.perform(post("/admin/user/reset-password")
                        .param("id", "no-such-user")
                        .with(user(TestUsers.admin())).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-error"));
    }

    @Test
    @DisplayName("admin 계정은 초기화되지 않는다")
    void adminAccountCannotBeReset() {
        String before = storedPassword("admin");

        AdminUserService.TempPassword reset = adminUserService.resetPassword("admin");

        assertThat(reset).isNull();
        assertThat(storedPassword("admin")).isEqualTo(before);
    }

    @Test
    @DisplayName("관리자가 아니면 초기화할 수 없다")
    void nonAdminCannotReset() throws Exception {
        mockMvc.perform(post("/admin/user/reset-password")
                        .param("id", "T-9105")
                        .with(user(TestUsers.noPermission())).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("사원 리스트에 초기화 단추가 있다")
    void listHasResetButton() throws Exception {
        String html = mockMvc.perform(get("/admin/user/list").with(user(TestUsers.admin())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("/admin/user/reset-password").contains("초기화");
    }

    private String storedPassword(String id) {
        return jdbc.queryForObject(
                "SELECT password FROM user_tbl WHERE id = ?", String.class, id);
    }

    private Integer changeRequired(String id) {
        return jdbc.queryForObject(
                "SELECT password_change_required FROM user_tbl WHERE id = ?", Integer.class, id);
    }

    private AdminUserEdit newUser(String id) {
        return new AdminUserEdit(id, "시험사원", "990115-1234567", id + "@erflow.test",
                "48058", "부산광역시", "3층", jobId(), deptId(), "123", "010-9999-8888");
    }

    private int jobId() {
        return jdbc.queryForObject("SELECT id FROM job_tbl WHERE id != -1 LIMIT 1", Integer.class);
    }

    private int deptId() {
        return jdbc.queryForObject("SELECT id FROM dept_tbl WHERE id != -1 LIMIT 1", Integer.class);
    }
}
