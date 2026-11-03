package com.erflow.admin.home;

import com.erflow.common.WorkStatus;
import com.erflow.post.PostListRow;
import com.erflow.post.PostSearch;
import com.erflow.post.PostService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 관리자 대시보드 업무.
 *
 * <p>한 화면에 네 곳의 값이 모인다 — 근무 현황(그래프)·결재·수발주·공지사항·출퇴근.
 * 레거시는 이 조회를 전부 {@code admin.jsp} 스크립틀릿에서 했다.
 */
@Service
public class AdminHomeService {

    /** 공지사항 게시판 번호. 레거시가 1 을 박아 두었다. */
    private static final int NOTICE_BOARD = 1;

    /** «새 글» 타일이 거슬러 올라가는 날수 (D-151). */
    private static final int NEW_POST_DAYS = 7;

    private final AdminHomeMapper mapper;

    private final PostService postService;

    private final Clock clock;

    /**
     * @param mapper 대시보드 매퍼
     * @param postService 게시글 업무. 공지사항 목록을 그대로 쓴다
     * @param clock 오늘 날짜의 출처. 시험에서 날짜를 고정하려고 주입받는다
     */
    public AdminHomeService(AdminHomeMapper mapper, PostService postService, Clock clock) {
        this.mapper = mapper;
        this.postService = postService;
        this.clock = clock;
    }

    /**
     * 대시보드 한 벌.
     *
     * @return 화면이 쓰는 네 목록
     */
    public Dashboard dashboard() {
        List<WorkRow> works = mapper.findWorks(today());
        return new Dashboard(
                mapper.findRecentProposals(),
                mapper.findRecentTasks(),
                postService.list(NOTICE_BOARD, PostSearch.none(), 1).rows(),
                works,
                stats(works));
    }

    /**
     * 숫자 타일 넷 (D-151). 오늘 출근은 그날 기록 중 결근·연차가 아닌 것을 센다 —
     * 출/퇴근관리 표의 시각이 찍히는 줄과 같은 판정({@link WorkStatus#clocked})이다.
     */
    private Stats stats(List<WorkRow> works) {
        int clockedIn = 0;
        for (WorkRow work : works) {
            if (WorkStatus.clocked(work.status())) {
                clockedIn++;
            }
        }
        return new Stats(clockedIn, mapper.countUsers(), mapper.countRunningDocuments(),
                mapper.countPostsSince(NEW_POST_DAYS));
    }

    /**
     * 근무 현황 그래프가 읽는 값.
     *
     * <p>레거시 {@code GraphWorkViewServlet} 이 하던 일이다 — 그날의 근무 기록을 상태별로
     * 세어 {@code {상태코드: 인원}} 으로 돌려준다.
     *
     * <p><b>없는 상태는 아예 빠진다.</b> 화면 쪽 스크립트가 그 값을 0번부터 이름표에
     * 짝지으므로, 빠진 상태가 있으면 조각 이름이 밀린다(D-069). 레거시 그대로다.
     *
     * @return 상태 코드별 인원. 코드 오름차순
     */
    public Map<Integer, Integer> workCounts() {
        Map<Integer, Integer> counts = new LinkedHashMap<>();
        for (WorkRow work : mapper.findWorks(today())) {
            counts.merge(work.status(), 1, Integer::sum);
        }
        // 레거시는 HashMap 을 그대로 JSON 으로 바꿨다. 작은 정수 키라 오름차순으로
        // 나왔고, 화면 스크립트가 그 순서에 기댄다. 순서를 고정해 둔다.
        return counts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(LinkedHashMap::new,
                        (map, entry) -> map.put(entry.getKey(), entry.getValue()),
                        LinkedHashMap::putAll);
    }

    private String today() {
        return LocalDate.now(clock).toString();
    }

    /**
     * 대시보드 한 벌.
     *
     * @param proposals 최근 결재
     * @param tasks 최근 수·발주
     * @param notices 공지사항
     * @param works 오늘의 근무 기록
     * @param stats 숫자 타일 넷
     */
    public record Dashboard(
            List<RecentProposal> proposals,
            List<RecentTask> tasks,
            List<PostListRow> notices,
            List<WorkRow> works,
            Stats stats) {
    }

    /**
     * 숫자 타일 넷 (D-151).
     *
     * @param clockedIn 오늘 출근한 사람 수 — 결근·연차가 아닌 근무 기록
     * @param headcount 사원 수
     * @param runningDocuments 결재가 돌고 있는 문서 수
     * @param newPosts 최근 7일 동안 올라온 글 수
     */
    public record Stats(int clockedIn, int headcount, int runningDocuments, int newPosts) {
    }
}
