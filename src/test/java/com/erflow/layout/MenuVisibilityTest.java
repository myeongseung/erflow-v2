package com.erflow.layout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.erflow.admin.permission.AdminPermissionService;
import com.erflow.auth.Permissions;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * 메뉴 표시가 프로그램 권한을 따르는지 확인한다 (D-135).
 *
 * <p>핵심은 <b>표시와 접근이 같은 값 하나</b>라는 것이다 — 보이는 메뉴는 반드시
 * 들어갈 수 있고, 못 들어가는 메뉴는 그려지지 않는다. 권한을 바꾸면 메뉴가 실제로
 * 사라지는 것까지 실제 DB 로 본다.
 */
@SpringBootTest(properties = "server.port=0")
@ActiveProfiles("local")
class MenuVisibilityTest {

    /** 관리자 비트만 빼고 전부 — 모든 프로그램에 들어가는 일반 사용자. */
    private static final long EVERY_BIT_BUT_ADMIN = Long.MAX_VALUE;

    @Autowired
    private MenuService menuService;

    @Autowired
    private AdminPermissionService permissionService;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeAll
    static void requireLocalConfig() {
        assumeTrue(
                new ClassPathResource("application-local.yml").exists(),
                "application-local.yml 이 없어 건너뛴다");
    }

    @Test
    @DisplayName("권한이 전혀 없으면 사이드바가 비고, 헤더에는 로그아웃만 남는다")
    void noPermissionSeesNothing() {
        List<MenuNode> side = menuService.sideMenu(false, 0L, 0L);
        List<MenuNode> header = menuService.headerMenu(false, 0L, 0L);

        assertThat(side).isEmpty();
        assertThat(header).extracting(MenuNode::label).containsExactly("로그아웃");
    }

    @Test
    @DisplayName("전 프로그램 권한이 있으면 레거시 사이드바 전부가 보인다")
    void fullPermissionSeesEverything() {
        List<MenuNode> side = menuService.sideMenu(
                false, EVERY_BIT_BUT_ADMIN, EVERY_BIT_BUT_ADMIN);

        assertThat(side).extracting(MenuNode::label).containsExactly(
                "문서 관리", "전자결재", "생산관리", "구매", "영업", "근태 관리", "게시판");
    }

    @Test
    @DisplayName("프로그램 권한을 잠그면 그 메뉴가 사라진다 — 그룹의 다른 메뉴는 남는다")
    @Transactional
    void lockingAProgramHidesItsMenu() {
        // «근태 확인» 을 관리자만 남기고 잠근다. 메뉴 관리 화면이 하는 일 그대로다.
        int programRowId = jdbc.queryForObject(
                "SELECT id FROM permission_program_tbl WHERE program_name = '근태 확인'",
                Integer.class);
        assertThat(permissionService.updateProgramDeptLevel(programRowId, List.of())).isTrue();

        List<MenuNode> side = menuService.sideMenu(
                false, EVERY_BIT_BUT_ADMIN, EVERY_BIT_BUT_ADMIN);

        // 하위가 «근태 확인» 하나뿐인 그룹(근태 관리)이라 그룹 제목까지 사라진다.
        assertThat(flatten(side)).doesNotContain("근태 확인").doesNotContain("근태 관리");
        assertThat(flatten(side)).contains("문서 관리", "기안 작성", "게시판");

        // 관리자에게는 그대로 보인다 — 잠근 값이 관리자 비트이기 때문이다.
        List<MenuNode> adminSide = menuService.sideMenu(
                true, Permissions.ADMIN_BIT, Permissions.ADMIN_BIT);
        assertThat(flatten(adminSide)).contains("근태 확인", "근태 관리");
    }

    @Test
    @DisplayName("보이는 메뉴는 반드시 들어갈 수 있다 — 표시와 접근이 같은 값이다")
    void visibleMenusAreAlwaysEnterable() {
        long dept = EVERY_BIT_BUT_ADMIN;
        long job = EVERY_BIT_BUT_ADMIN;

        for (MenuNode root : menuService.sideMenu(false, dept, job)) {
            for (MenuNode item : root.children().isEmpty() ? List.of(root) : root.children()) {
                if (item.screenId() == null) {
                    continue;
                }
                var levels = jdbc.queryForMap(
                        "SELECT COALESCE(l.dept_level, 0) AS dept_level, "
                                + "COALESCE(l.job_level, 0) AS job_level "
                                + "FROM screen s "
                                + "LEFT JOIN permission_program_tbl l "
                                + "ON l.program_id = s.program_id "
                                + "WHERE s.screen_id = ?", item.screenId());
                assertThat(Permissions.hasProgramPermission(dept, job,
                        ((Number) levels.get("dept_level")).longValue(),
                        ((Number) levels.get("job_level")).longValue()))
                        .as("보이는 메뉴 «%s» 는 들어갈 수 있어야 한다", item.label())
                        .isTrue();
            }
        }
    }

    private static List<String> flatten(List<MenuNode> tree) {
        List<String> labels = new ArrayList<>();
        for (MenuNode root : tree) {
            labels.add(root.label());
            for (MenuNode child : root.children()) {
                labels.add(child.label());
            }
        }
        return labels;
    }
}
