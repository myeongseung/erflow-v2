package com.erflow.admin.board;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.erflow.admin.permission.PermissionChoice;
import com.erflow.auth.Permissions;
import com.erflow.auth.TestUsers;
import com.erflow.post.Board;
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
 * 게시판 관리 화면이 실제 데이터로 도는지 확인한다.
 *
 * <p>게시판 권한도 비트마스크다. <b>고치지 않는 쪽이 지워지지 않는지</b>가 이 화면의
 * 핵심이다 — 갱신 문장이 부서·직급 두 칸을 함께 쓰기 때문에, 읽어 온 값을 되돌려
 * 놓지 않으면 부서를 고칠 때마다 직급 권한이 사라진다. 게이트는 못 본다.
 */
@SpringBootTest(properties = "server.port=0")
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AdminBoardScreenTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminBoardService boardService;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeAll
    static void requireLocalConfig() {
        assumeTrue(
                new ClassPathResource("application-local.yml").exists(),
                "application-local.yml 이 없어 건너뛴다");
    }

    @Test
    @DisplayName("게시판 관리 화면이 표 둘로 그려진다")
    void listRenders() throws Exception {
        String html = mockMvc.perform(get("/admin/board/list").with(user(TestUsers.admin())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("게시판 목록").contains("게시글 목록")
                .contains("최초 생성시각").contains("부서 읽기").contains("직급 쓰기")
                .contains("조회수").contains("댓글수");
    }

    @Test
    @DisplayName("공지사항과 자유게시판에는 삭제 체크박스가 없다")
    void defaultBoardsCannotBeDeleted() {
        var rows = boardService.list("", 1).rows();

        assertThat(rows).isNotEmpty();
        for (AdminBoardRow row : rows) {
            assertThat(row.deletable()).isEqualTo(row.id() > 2);
        }
    }

    @Test
    @DisplayName("새 게시판은 관리자만 읽고 쓸 수 있다")
    @Transactional
    void newBoardStartsAdminOnly() {
        assertThat(boardService.create("시험게시판")).isTrue();

        int id = jdbc.queryForObject(
                "SELECT id FROM board_tbl WHERE subject = '시험게시판'", Integer.class);
        Board board = boardService.get(id);
        assertThat(board.readDeptLevel()).isEqualTo(Permissions.ADMIN_BIT);
        assertThat(board.readJobLevel()).isEqualTo(Permissions.ADMIN_BIT);
        assertThat(board.writeDeptLevel()).isEqualTo(Permissions.ADMIN_BIT);
        assertThat(board.writeJobLevel()).isEqualTo(Permissions.ADMIN_BIT);
    }

    @Test
    @DisplayName("같은 이름의 게시판은 만들 수 없다")
    @Transactional
    void duplicateNameIsRejected() {
        String existing = boardService.list("", 1).rows().get(0).subject();

        assertThat(boardService.create(existing)).isFalse();
    }

    @Test
    @DisplayName("부서 권한을 고쳐도 직급 권한이 지워지지 않는다")
    @Transactional
    void changingDeptKeepsJob() {
        int boardId = newBoardId();
        // 직급 쪽에 값을 하나 넣어 둔다.
        var jobs = jdbc.queryForList(
                "SELECT level FROM permission_job_tbl LIMIT 1", Long.class);
        long jobLevel = Permissions.ADMIN_BIT | jobs.get(0);
        jdbc.update("UPDATE board_tbl SET permission_read_job_level = ? WHERE id = ?",
                jobLevel, boardId);

        var depts = jdbc.queryForList(
                "SELECT dept_id FROM permission_dept_view WHERE dept_id != -1 LIMIT 1",
                Integer.class);
        assertThat(boardService.updatePermission(boardId, false, false, depts)).isTrue();

        Board board = boardService.get(boardId);
        // 부서는 «관리자 + 체크한 것» 으로 바뀌고,
        assertThat(board.readDeptLevel() & Permissions.ADMIN_BIT)
                .isEqualTo(Permissions.ADMIN_BIT);
        // 직급은 손대지 않은 값 그대로다.
        assertThat(board.readJobLevel()).isEqualTo(jobLevel);
    }

    @Test
    @DisplayName("읽기 권한을 고쳐도 쓰기 권한이 지워지지 않는다")
    @Transactional
    void changingReadKeepsWrite() {
        int boardId = newBoardId();
        jdbc.update("UPDATE board_tbl SET permission_write_dept_level = 7 WHERE id = ?", boardId);

        boardService.updatePermission(boardId, false, false, List.of());

        assertThat(boardService.get(boardId).writeDeptLevel()).isEqualTo(7L);
        // 체크를 다 지워도 관리자는 남는다.
        assertThat(boardService.get(boardId).readDeptLevel()).isEqualTo(Permissions.ADMIN_BIT);
    }

    @Test
    @DisplayName("권한 수정 화면은 이미 가진 권한에 체크를 켠다")
    @Transactional
    void permissionFormChecksGranted() {
        int boardId = newBoardId();
        Integer deptId = jdbc.queryForObject(
                "SELECT dept_id FROM permission_dept_view WHERE dept_id != -1 LIMIT 1",
                Integer.class);
        boardService.updatePermission(boardId, false, false, List.of(deptId));

        var form = boardService.permissionForm(boardId, false, false);

        assertThat(form.choices()).isNotEmpty();
        assertThat(form.choices().stream()
                .filter(PermissionChoice::checked)
                .map(PermissionChoice::classId))
                .containsExactly(deptId);
    }

    @Test
    @DisplayName("게시판을 지우면 그 안의 글도 함께 사라진다")
    @Transactional
    void deletingBoardRemovesItsPosts() {
        int boardId = newBoardId();
        jdbc.update("INSERT INTO post_tbl (user_tbl_id, board_tbl_id, subject, content, "
                + "created_at, count, depth, pos, post_tbl_ref_id) "
                + "VALUES ('admin', ?, '시험글', '내용', NOW(), 0, 0, 0, 0)", boardId);

        assertThat(boardService.delete(List.of(boardId))).isTrue();

        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM board_tbl WHERE id = ?", Integer.class, boardId)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM post_tbl WHERE board_tbl_id = ?",
                Integer.class, boardId)).isZero();
    }

    @Test
    @DisplayName("권한 수정 화면은 flag 가 없으면 잘못된 접근으로 보낸다")
    void permissionFormWithoutFlagRedirects() throws Exception {
        mockMvc.perform(get("/admin/board/dept-update").param("id", "1")
                        .with(user(TestUsers.admin())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-error"));
    }

    @Test
    @DisplayName("관리자가 아니면 게시판 관리 화면을 볼 수 없다")
    void nonAdminIsBlocked() throws Exception {
        mockMvc.perform(get("/admin/board/list").with(user(TestUsers.noPermission())))
                .andExpect(status().isForbidden());
    }

    private int newBoardId() {
        boardService.create("시험게시판");
        return jdbc.queryForObject(
                "SELECT id FROM board_tbl WHERE subject = '시험게시판'", Integer.class);
    }
}
