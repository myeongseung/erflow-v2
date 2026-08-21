package com.erflow.index;

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

/**
 * 메인 화면이 실제 데이터로 그려지는지 확인한다.
 *
 * <p>게이트가 못 보는 자리가 둘이다. <b>위젯마다 자르는 규칙이 다르다</b>는 것과,
 * <b>권한을 묻지 않는다</b>는 것이다. 둘 다 화면 구조는 같고 내용만 달라진다.
 */
@SpringBootTest(properties = "server.port=0")
@AutoConfigureMockMvc
@ActiveProfiles("local")
class IndexScreenTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IndexService indexService;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeAll
    static void requireLocalConfig() {
        assumeTrue(
                new ClassPathResource("application-local.yml").exists(),
                "application-local.yml 이 없어 건너뛴다");
    }

    @Test
    @DisplayName("메인 화면에 위젯 넷이 그려진다")
    void widgetsRender() throws Exception {
        String html = mockMvc.perform(get("/index").with(user(TestUsers.admin())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("공지사항").contains("전자결재")
                .contains("자유게시판").contains("받은 쪽지함").contains("일정관리")
                // 아래쪽 회사 정보까지 한 화면이다
                .contains("Dong-eui University");
    }

    @Test
    @DisplayName("개발자 PC 주소가 화면에 없다 — D-085 를 2단계에서 지웠다(D-114)")
    void developerAddressIsGone() throws Exception {
        // 레거시 달력 위젯의 새로고침이 만든 사람 PC(localhost:5501)를 가리켰다.
        String html = mockMvc.perform(get("/index").with(user(TestUsers.admin())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).doesNotContain("localhost:5501");
    }

    @Test
    @DisplayName("«/» 로 들어가도 같은 화면이다")
    void rootIsTheSameScreen() throws Exception {
        mockMvc.perform(get("/").with(user(TestUsers.admin())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("권한이 하나도 없어도 메인은 열린다 — 로그인만 본다")
    void noPermissionStillSeesTheMainScreen() throws Exception {
        mockMvc.perform(get("/index").with(user(TestUsers.noPermission())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("메인은 보이고, 위젯의 목적지는 문에서 막는다 (D-084·D-115)")
    void widgetsShowButTheirTargetsBlock() throws Exception {
        // 2단계에서 «위젯마다 권한을 본다» 안을 종결하며 이 짝을 못 박는다:
        // 메인은 모두에게 같은 모습이고, 눌러 들어가는 화면이 권한을 막는다.
        String html = mockMvc.perform(get("/index").with(user(TestUsers.noPermission())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("공지사항").contains("전자결재")
                .contains("자유게시판").contains("받은 쪽지함");

        // 게시판은 권한이 없으면 권한 화면으로 되돌린다(리다이렉트).
        mockMvc.perform(get("/post/list").param("boardId", "1")
                        .with(user(TestUsers.noPermission())))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("자유게시판만 작성자 이름을 자른다 — 공지사항은 그대로 둔다")
    void onlyTheFreeBoardCutsTheAuthorName() {
        String longName = jdbc.queryForObject(
                "SELECT name FROM user_tbl WHERE CHAR_LENGTH(name) > 3 LIMIT 1", String.class);
        assumeTrue(longName != null, "세 글자가 넘는 이름이 없어 건너뛴다");

        assertThat(IndexService.cut(longName, 3)).endsWith("...");
        // 같은 이름이 공지사항 위젯에서는 잘리지 않는다.
        assertThat(indexService.posts(IndexService.NOTICE_BOARD, false))
                .allSatisfy(row -> assertThat(row.name()).doesNotEndWith("..."));
    }

    @Test
    @DisplayName("제목은 열 글자에서 잘린다")
    void subjectIsCutAtTen() {
        assertThat(IndexService.cut("가나다라마바사아자차카", 10)).isEqualTo("가나다라마바사아자차...");
        assertThat(IndexService.cut("가나다라마바사아자차", 10)).isEqualTo("가나다라마바사아자차");
        assertThat(IndexService.cut(null, 10)).isNull();
    }

    @Test
    @DisplayName("번호는 글번호가 아니라 «몇 번째로 최신인가» 다")
    void numberCountsDownFromTheTotal() {
        List<IndexPostRow> rows = indexService.posts(IndexService.NOTICE_BOARD, false);
        assumeTrue(!rows.isEmpty(), "공지사항이 없어 건너뛴다");

        int total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM post_view WHERE board_id = 1", Integer.class);
        assertThat(rows.get(0).number()).isEqualTo(total);
        for (int i = 0; i < rows.size(); ++i) {
            assertThat(rows.get(i).number()).isEqualTo(total - i);
        }
    }

    @Test
    @DisplayName("위젯은 열다섯 줄까지만 보여준다")
    void widgetShowsFifteenAtMost() {
        assertThat(indexService.posts(IndexService.FREE_BOARD, true)).hasSizeLessThanOrEqualTo(15);
        assertThat(indexService.messages("admin")).hasSizeLessThanOrEqualTo(15);
        assertThat(indexService.proposals("admin")).hasSizeLessThanOrEqualTo(15);
    }

    @Test
    @DisplayName("전자결재 위젯은 «내 차례» 를 거르지 않는다 — 결재 리스트와 다르다")
    void proposalWidgetDoesNotFilterByTurn() {
        String owner = jdbc.queryForObject(
                "SELECT user_tbl_id FROM proposal_tbl LIMIT 1", String.class);
        assumeTrue(owner != null, "결재가 없어 건너뛴다");

        int widget = indexService.proposals(owner).size();
        int expected = jdbc.queryForObject(
                "SELECT COUNT(*) FROM (SELECT id FROM recent_proposal_view "
                        + "WHERE original_user = ? OR (user_id = ? AND result = 3) "
                        + "ORDER BY id DESC LIMIT 0, 15) t",
                Integer.class, owner, owner);

        assertThat(widget).isEqualTo(expected);
    }

    @Test
    @DisplayName("받은 쪽지함은 내가 받은 것만 보여준다")
    void inboxShowsOnlyMine() {
        String receiver = jdbc.queryForObject(
                "SELECT receiver_id FROM message_view LIMIT 1", String.class);
        assumeTrue(receiver != null, "쪽지가 없어 건너뛴다");

        assertThat(indexService.messages(receiver)).isNotEmpty();
        assertThat(indexService.messages("no-such-user")).isEmpty();
    }

    @Test
    @DisplayName("보낸이는 «[부서] 이름 직급» 으로 찍힌다")
    void senderIsLabelled() {
        String receiver = jdbc.queryForObject(
                "SELECT receiver_id FROM message_view LIMIT 1", String.class);
        assumeTrue(receiver != null, "쪽지가 없어 건너뛴다");

        assertThat(indexService.messages(receiver))
                .allSatisfy(row -> assertThat(row.sender()).startsWith("["));
    }
}
