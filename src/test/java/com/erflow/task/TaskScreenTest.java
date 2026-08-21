package com.erflow.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import com.erflow.auth.TestUsers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 수·발주 목록이 실제 데이터로 그려지는지 확인한다.
 *
 * <p>연결된 MariaDB 를 상대로 돈다. 발주(type=1)와 수주(type=0)가 서로 다른 목록을
 * 보여주는지, 검색이 대상 칸만 훑는지를 주로 본다.
 */
@SpringBootTest(properties = "server.port=0")
@AutoConfigureMockMvc
@ActiveProfiles("local")
class TaskScreenTest {

    private static final Path OUT = Path.of("build", "rendered");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private DataSource dataSource;

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
    @DisplayName("발주 목록이 그려지고 화면 글자가 발주다")
    void purchaseListRenders() throws Exception {
        String html = render("/task/purchase-task", "task-purchase.html");

        assertThat(html).contains("발주 관리").contains("의뢰 시각");
        assertThat(html).doesNotContain("수주 시각");
    }

    @Test
    @DisplayName("수주 목록이 그려지고 화면 글자가 수주다")
    void sellListRenders() throws Exception {
        String html = render("/task/sell-task", "task-sell.html");

        assertThat(html).contains("수주 관리").contains("수주 시각");
    }

    @Test
    @DisplayName("발주와 수주는 서로 다른 목록이다")
    void purchaseAndSellAreDifferentSets() {
        List<TaskRow> purchase = taskService.list(
                TaskService.PURCHASE, TaskSearch.none(), 1).rows();
        List<TaskRow> sell = taskService.list(
                TaskService.SELL, TaskSearch.none(), 1).rows();

        // 같은 화면 골격을 쓰지만 type 으로 갈린다. 한쪽 id 가 다른 쪽에 섞이면
        // type 필터가 빠진 것이다.
        assertThat(purchase).isNotEmpty();
        List<Integer> sellIds = sell.stream().map(TaskRow::id).toList();
        assertThat(purchase).noneMatch(row -> sellIds.contains(row.id()));
    }

    @Test
    @DisplayName("담당직원명으로 걸러낸다")
    void filtersByUserName() {
        var all = taskService.list(TaskService.PURCHASE, TaskSearch.none(), 1).rows();
        assumeTrue(!all.isEmpty(), "발주 데이터가 없어 건너뛴다");
        String name = all.get(0).userName();

        var filtered = taskService.list(
                TaskService.PURCHASE, new TaskSearch("userName", name), 1).rows();

        assertThat(filtered).isNotEmpty()
                .allSatisfy(row -> assertThat(row.userName()).contains(name));
    }

    @Test
    @DisplayName("검색 대상이 아닌 화이트리스트 밖 keyfield 는 전체 조회로 떨어진다")
    void unknownKeyfieldFallsBackToAll() {
        var all = taskService.list(TaskService.PURCHASE, TaskSearch.none(), 1)
                .pagination().totalRecord();
        var junk = taskService.list(
                TaskService.PURCHASE, new TaskSearch("id; DROP TABLE task_tbl --", "x"), 1)
                .pagination().totalRecord();

        // 화이트리스트 밖이면 조건을 만들지 않는다 — 전체 건수가 같아야 한다.
        assertThat(junk).isEqualTo(all);
    }

    @Test
    @DisplayName("상태 검색어가 숫자가 아니면 목록이 빈다")
    void nonNumericStatusYieldsEmpty() {
        // 레거시는 Integer.parseInt 예외를 삼켜 빈 목록을 냈다(TaskSearch.isBrokenStatus).
        var page = taskService.list(
                TaskService.PURCHASE, new TaskSearch("status", "진행중"), 1);

        assertThat(page.rows()).isEmpty();
        assertThat(page.pagination().totalRecord()).isZero();
    }

    @Test
    @DisplayName("발주 등록 화면이 그려진다")
    void purchaseRegisterFormRenders() throws Exception {
        String html = render("/task/register?flag=purchase", "task-register.html");

        assertThat(html).contains("발주 등록").contains("직원 찾기").contains("제품 찾기");
        assertThat(html).contains("진행중").contains("완료").contains("미확인");
    }

    @Test
    @DisplayName("수주 등록 화면은 글자가 수주다")
    void sellRegisterFormRenders() throws Exception {
        String html = render("/task/register?flag=sell", "task-register-sell.html");

        assertThat(html).contains("수주 등록");
    }

    @Test
    @DisplayName("flag 가 없으면 화면 권한 판정이 먼저 막는다")
    void registerWithoutFlagIsDenied() throws Exception {
        // screen 테이블에 /task/register 는 flag=sell·purchase 행만 있다. flag 가 없으면
        // 어느 규칙에도 안 걸려 ScreenAuthorizationManager 가 거부한다(컨트롤러 전에 403).
        // 레거시는 no-flag 를 accessError 로 보냈지만, 화면 권한을 한 곳에서 판정하는
        // 우리 구조는 permission 계열로 막는다 — 협력업체 flag 처리(D-018)와 같은 맥락.
        mockMvc.perform(get("/task/register").with(user(TestUsers.admin())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("내역 모달은 taskId 가 없으면 잘못된 접근으로 보낸다")
    void modalWithoutTaskIdRedirects() throws Exception {
        mockMvc.perform(get("/task/history-modal").with(user(TestUsers.admin())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-error"));
    }

    @Test
    @DisplayName("등록하면 수량 0 인 제품은 이력에서 빠진다")
    @Transactional
    void createStoresOnlyPositiveCounts() throws Exception {
        long[] refs = existingTaskRefs();
        assumeTrue(refs != null, "기존 수·발주가 없어 건너뛴다");
        List<String> products = someProducts(2);
        assumeTrue(products.size() == 2, "제품이 둘 미만이라 건너뛴다");

        boolean created = taskService.create(
                new Task((String) existingUserId, (int) refs[0], (int) refs[1],
                        TaskService.PURCHASE, "2026-01-01 00:00:00", 1),
                List.of(new TaskHistory(0, products.get(0), 5),
                        new TaskHistory(0, products.get(1), 0)));

        assertThat(created).isTrue();
        int newId = taskMapper.lastInsertId();
        // 수량 5 만 남고 0 은 빠진다 — 레거시 if(count > 0) 그대로.
        assertThat(taskService.histories(newId))
                .extracting(TaskHistoryRow::productId)
                .containsExactly(products.get(0));
    }

    /** FK 안전한 값을 기존 task_tbl 한 행에서 가져온다. */
    private Object existingUserId;

    private long[] existingTaskRefs() throws Exception {
        try (Connection c = dataSource.getConnection();
                Statement s = c.createStatement();
                ResultSet rs = s.executeQuery(
                        "SELECT user_tbl_id, company_tbl_id, document_tbl_id FROM task_tbl LIMIT 1")) {
            if (!rs.next()) {
                return null;
            }
            existingUserId = rs.getString(1);
            return new long[] {rs.getLong(2), rs.getLong(3)};
        }
    }

    private List<String> someProducts(int n) throws Exception {
        List<String> ids = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
                Statement s = c.createStatement();
                ResultSet rs = s.executeQuery("SELECT id FROM product_tbl LIMIT " + n)) {
            while (rs.next()) {
                ids.add(rs.getString(1));
            }
        }
        return ids;
    }

    private Integer firstTaskId(int type) throws Exception {
        try (Connection c = dataSource.getConnection();
                Statement s = c.createStatement();
                ResultSet rs = s.executeQuery(
                        "SELECT id FROM task_tbl WHERE type = " + type + " LIMIT 1")) {
            return rs.next() ? rs.getInt(1) : null;
        }
    }

    @Test
    @DisplayName("발주 수정 화면이 기존 값으로 그려진다")
    void purchaseUpdateFormRenders() throws Exception {
        Integer id = firstTaskId(TaskService.PURCHASE);
        assumeTrue(id != null, "발주 데이터가 없어 건너뛴다");

        String html = render("/task/update?flag=purchase&id=" + id, "task-update.html");

        assertThat(html).contains("발주 관리 수정").contains("제출").contains("제품 찾기");
    }

    @Test
    @DisplayName("수정 화면은 id 가 없으면 잘못된 접근으로 보낸다")
    void updateWithoutIdRedirects() throws Exception {
        mockMvc.perform(get("/task/update?flag=purchase").with(user(TestUsers.admin())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/access-error"));
    }

    @Test
    @DisplayName("수주 수정은 성공한다")
    @Transactional
    void sellUpdateSucceeds() throws Exception {
        Integer id = firstTaskId(TaskService.SELL);
        assumeTrue(id != null, "수주 데이터가 없어 건너뛴다");
        List<String> products = someProducts(1);
        assumeTrue(!products.isEmpty(), "제품이 없어 건너뛴다");
        TaskDetail before = taskService.get(id);

        boolean updated = taskService.update(
                new TaskUpdate(id, before.userId(), before.documentId(), "2026-02-02 00:00:00", 2, 0),
                List.of(new TaskHistory(0, products.get(0), 3)));

        assertThat(updated).isTrue();
    }

    @Test
    @DisplayName("발주 수정이 성공한다 — D-044 를 2단계에서 고쳤다(D-106)")
    @Transactional
    void purchaseUpdateSucceedsNow() throws Exception {
        Integer id = firstTaskId(TaskService.PURCHASE);
        assumeTrue(id != null, "발주 데이터가 없어 건너뛴다");
        List<String> products = someProducts(1);
        assumeTrue(!products.isEmpty(), "제품이 없어 건너뛴다");
        TaskDetail before = taskService.get(id);

        // 레거시는 type 을 안 넣어(언제나 0) 발주(type=1) 수정이 늘 실패했다(D-044).
        // 이제 컨트롤러가 flag 로 type 을 채워 넘긴다.
        boolean updated = taskService.update(
                new TaskUpdate(id, before.userId(), before.documentId(),
                        "2026-02-02 00:00:00", 2, TaskService.PURCHASE),
                List.of(new TaskHistory(0, products.get(0), 7)));

        assertThat(updated).isTrue();
        assertThat(taskService.histories(id))
                .extracting(TaskHistoryRow::productId)
                .containsExactly(products.get(0));
    }

    @Test
    @DisplayName("본체 수정이 실패하면 이력도 그대로다 (D-106)")
    @Transactional
    void failedUpdateLeavesHistoriesAlone() throws Exception {
        Integer id = firstTaskId(TaskService.PURCHASE);
        assumeTrue(id != null, "발주 데이터가 없어 건너뛴다");
        List<String> products = someProducts(1);
        assumeTrue(!products.isEmpty(), "제품이 없어 건너뛴다");
        TaskDetail before = taskService.get(id);
        List<String> beforeHistories = taskService.histories(id).stream()
                .map(TaskHistoryRow::productId).toList();

        // type 이 어긋나면 본체가 0 행 — 레거시는 그래도 이력을 갈아 끼웠지만(D-044),
        // 이제 실패한 수정은 아무것도 바꾸지 않는다.
        boolean updated = taskService.update(
                new TaskUpdate(id, before.userId(), before.documentId(),
                        "2026-02-02 00:00:00", 2, TaskService.SELL),
                List.of(new TaskHistory(0, products.get(0), 7)));

        assertThat(updated).isFalse();
        assertThat(taskService.histories(id))
                .extracting(TaskHistoryRow::productId)
                .containsExactlyElementsOf(beforeHistories);
    }

    @Test
    @DisplayName("문서 없는 수·발주는 «null» 이 아니라 빈칸이다 — D-089 를 2단계에서 고쳤다(D-112)")
    void missingDocumentShowsBlankNow() {
        // 레거시는 <%=subject%> 가 null 을 «null» 네 글자로 찍었다.
        assertThat(new TaskRow(1, "이름", "부서", "회사", null, "2023-01-01", 0)
                .subjectLabel()).isEmpty();
        assertThat(new TaskRow(1, "이름", "부서", "회사", "제목", "2023-01-01", 0)
                .subjectLabel()).isEqualTo("제목");
    }

    @Test
    @DisplayName("삭제는 type 이 맞아야 지운다")
    @Transactional
    void deleteRespectsType() throws Exception {
        Integer sellId = firstTaskId(TaskService.SELL);
        assumeTrue(sellId != null, "수주 데이터가 없어 건너뛴다");

        // 수주 건을 발주 type 으로 지우려 하면 걸리는 행이 없다.
        assertThat(taskService.delete(List.of(sellId), TaskService.PURCHASE)).isFalse();
        // 올바른 type 이면 지워진다.
        assertThat(taskService.delete(List.of(sellId), TaskService.SELL)).isTrue();
    }
}
