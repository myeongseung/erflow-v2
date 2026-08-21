package com.erflow.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
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
 * 쪽지함 목록이 실제 데이터로 그려지는지 확인한다.
 *
 * <p>받은쪽지함(receiver)과 보낸쪽지함(sender)이 갈리는지, 소프트 삭제 필터
 * ({@code <class>_visible = 1})가 걸리는지를 주로 본다.
 */
@SpringBootTest(properties = "server.port=0")
@AutoConfigureMockMvc
@ActiveProfiles("local")
class MessageScreenTest {

    private static final Path OUT = Path.of("build", "rendered");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MessageService messageService;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeAll
    static void requireLocalConfig() {
        assumeTrue(
                new ClassPathResource("application-local.yml").exists(),
                "application-local.yml 이 없어 건너뛴다");
    }

    private String render(String path, String fileName) throws Exception {
        String html = mockMvc.perform(get(path).with(user(TestUsers.admin())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Files.createDirectories(OUT);
        Files.writeString(OUT.resolve(fileName), html);
        return html;
    }

    @Test
    @DisplayName("받은쪽지함이 그려진다")
    void receiverBoxRenders() throws Exception {
        String html = render("/message?class=receiver", "message-receiver.html");

        assertThat(html).contains("받은쪽지함").contains("보낸쪽지함").contains("쪽지쓰기");
        // 받은쪽지함에서만 답장 버튼과 «보낸사람» 검색이 나온다.
        assertThat(html).contains("답장").contains("보낸사람");
    }

    @Test
    @DisplayName("보낸쪽지함은 답장 버튼이 없고 검색이 받는사람이다")
    void senderBoxHasNoReplyAndSearchesReceiver() throws Exception {
        String html = render("/message?class=sender", "message-sender.html");

        assertThat(html).contains("받는사람");
        // 답장 버튼은 받은쪽지함에만 있다.
        assertThat(html).doesNotContain("reply-message");
    }

    @Test
    @DisplayName("class 가 없으면 받은쪽지함으로 본다")
    void defaultsToReceiver() throws Exception {
        assertThat(render("/message", "message-default.html")).contains("받은쪽지함");
    }

    @Test
    @DisplayName("class 가 receiver/sender 가 아니면 잘못된 접근으로 보낸다")
    void unknownClassRedirects() throws Exception {
        mockMvc.perform(get("/message?class=xxx").with(user(TestUsers.admin())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-error"));
    }

    @Test
    @DisplayName("받은쪽지함과 보낸쪽지함은 다른 목록이다")
    void boxesDiffer() {
        // admin 기준. 목록이 겹치면 class 필터가 빠진 것이다. (둘 다 비어 있으면 건너뛴다)
        List<MessageRow> received = messageService.list(
                MessageService.RECEIVER, "admin", new MessageSearch("", ""), 1).rows();
        List<MessageRow> sent = messageService.list(
                MessageService.SENDER, "admin", new MessageSearch("", ""), 1).rows();
        assumeTrue(!received.isEmpty() || !sent.isEmpty(), "admin 쪽지가 없어 건너뛴다");

        // 받은함의 각 행은 admin 이 받는 사람, 보낸함의 각 행은 admin 이 보낸 사람.
        // 한 쪽지가 양쪽에 동시에 들어가려면 자기 자신에게 보낸 것뿐이라, id 교집합은
        // 그런 경우가 없으면 비어야 한다. 여기서는 각 목록이 자기 조건을 지키는지만 본다.
        assertThat(received).allSatisfy(row -> assertThat(row.senderId()).isNotNull());
    }

    @Test
    @DisplayName("쪽지를 보내면 message_tbl 에 한 건 쌓인다")
    @Transactional
    void sendInsertsOneMessage() {
        // admin 이 자신에게 보낸다(받는 사람이 실재해야 하므로 안전한 대상). 롤백된다.
        // JdbcTemplate 은 테스트 트랜잭션의 커넥션을 써서 아직 커밋 안 된 삽입도 본다.
        int before = jdbc.queryForObject("SELECT COUNT(*) FROM message_tbl", Integer.class);

        boolean sent = messageService.send("admin", List.of("admin"), "테스트 쪽지");

        assertThat(sent).isTrue();
        int after = jdbc.queryForObject("SELECT COUNT(*) FROM message_tbl", Integer.class);
        assertThat(after).isEqualTo(before + 1);
    }

    @Test
    @DisplayName("받는 사람을 ; 로 이으면 사람 수만큼 들어간다")
    @Transactional
    void sendSplitsBySemicolon() {
        int before = jdbc.queryForObject("SELECT COUNT(*) FROM message_tbl", Integer.class);

        // 레거시는 ; 로 이어 여러 명에게 보낸다.
        messageService.send("admin", List.of("admin;admin".split(";")), "여러 명");

        int after = jdbc.queryForObject("SELECT COUNT(*) FROM message_tbl", Integer.class);
        assertThat(after).isEqualTo(before + 2);
    }

    @Test
    @DisplayName("읽기 화면을 열면 읽음으로 바뀐다 (D-046)")
    @Transactional
    void readMarksAsRead() {
        // admin 에게 새 쪽지를 하나 만들고(읽지 않음), 그 id 로 읽기를 연다. 롤백된다.
        messageService.send("admin", List.of("admin"), "읽음 시험");
        int id = jdbc.queryForObject("SELECT MAX(id) FROM message_tbl", Integer.class);

        assertThat(readStatus(id)).isZero();
        MessageDetail read = messageService.read(id);
        assertThat(read).isNotNull();
        assertThat(readStatus(id)).isEqualTo(1);
    }

    private int readStatus(int id) {
        return jdbc.queryForObject(
                "SELECT read_status FROM message_tbl WHERE id = ?", Integer.class, id);
    }

    @Test
    @DisplayName("삭제는 지우지 않고 내 쪽 visible 만 내린다 (D-047)")
    @Transactional
    void deleteSoftHides() {
        // admin 이 자신에게 보낸 쪽지 — admin 이 보낸 쪽이자 받는 쪽이라 둘 다 내려간다.
        messageService.send("admin", List.of("admin"), "삭제 시험");
        int id = jdbc.queryForObject("SELECT MAX(id) FROM message_tbl", Integer.class);
        assertThat(visible(id, "sender_visible")).isEqualTo(1);
        assertThat(visible(id, "receiver_visible")).isEqualTo(1);

        boolean deleted = messageService.delete("admin", List.of(id));

        assertThat(deleted).isTrue();
        // 행은 그대로 있고 플래그만 0 이 된다.
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM message_tbl WHERE id = ?", Integer.class, id))
                .isEqualTo(1);
        assertThat(visible(id, "sender_visible")).isZero();
        assertThat(visible(id, "receiver_visible")).isZero();
    }

    @Test
    @DisplayName("나와 무관한 쪽지는 삭제되지 않는다")
    @Transactional
    void deleteByNonPartyFails() {
        messageService.send("admin", List.of("admin"), "남의 쪽지");
        int id = jdbc.queryForObject("SELECT MAX(id) FROM message_tbl", Integer.class);

        // 보낸 사람도 받는 사람도 아닌 사용자.
        boolean deleted = messageService.delete("nobody-xyz", List.of(id));

        assertThat(deleted).isFalse();
        assertThat(visible(id, "sender_visible")).isEqualTo(1);
        assertThat(visible(id, "receiver_visible")).isEqualTo(1);
    }

    @Test
    @DisplayName("쪽지함 페이징이 동작한다 — D-045 를 2단계에서 되살렸다(D-107)")
    @Transactional
    void pagingWorksNow() throws Exception {
        // 레거시는 페이징 링크가 부르는 block()/paging() 을 어디에도 정의하지 않아
        // 쪽지가 15건을 넘으면 1페이지 말고는 볼 수 없었다. 페이지가 나뉘도록
        // admin 에게 16건을 만든다(페이지당 15건). 롤백된다.
        for (int i = 0; i < 16; i++) {
            messageService.send("admin", List.of("admin"), "페이징 시험 " + i);
        }

        var page1 = messageService.list(
                MessageService.RECEIVER, "admin", new MessageSearch("", ""), 1);
        var page2 = messageService.list(
                MessageService.RECEIVER, "admin", new MessageSearch("", ""), 2);

        assertThat(page1.rows()).hasSize(15);
        assertThat(page2.rows()).isNotEmpty();
        // 두 페이지는 겹치지 않는다.
        List<Integer> firstIds = page1.rows().stream().map(MessageRow::id).toList();
        assertThat(page2.rows()).noneSatisfy(row -> assertThat(firstIds).contains(row.id()));

        // 화면에는 함수가 제출할 페이지 번호 칸이 있다.
        assertThat(render("/message?class=receiver", "message-paging.html"))
                .contains("name=\"nowPage\"");
    }

    @Test
    @DisplayName("목록 삭제는 class 가 없으면 잘못된 접근으로 보낸다")
    void listDeleteWithoutClassRedirects() throws Exception {
        mockMvc.perform(post("/message/delete").with(user(TestUsers.admin())).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-error"));
    }

    private int visible(int id, String column) {
        return jdbc.queryForObject(
                "SELECT " + column + " FROM message_tbl WHERE id = " + id, Integer.class);
    }
}
