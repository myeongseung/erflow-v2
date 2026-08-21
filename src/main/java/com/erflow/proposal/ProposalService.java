package com.erflow.proposal;

import com.erflow.common.Pagination;
import com.erflow.user.UserMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결재 업무.
 *
 * <p>레거시는 페이징 계산을 {@code proposalList.jsp} 스크립틀릿에, DB 접근을
 * {@code ProposalServiceImpl} 에 두었다. 화면에서 로직을 걷어내 여기로 모은다.
 */
@Service
public class ProposalService {

    /** 결재진행중. 레거시 {@code result} 3. */
    public static final int IN_PROGRESS = 3;

    /** 승인. 레거시 {@code result} 1. */
    public static final int APPROVED = 1;

    /** 반려. 레거시 {@code result} 2. */
    public static final int REJECTED = 2;

    /** 내 차례가 끝났다. 레거시 {@code result} 0 — 어떤 목록 필터에도 걸리지 않는다. */
    public static final int PASSED = 0;

    /** 승인 버튼이 보내는 값. 레거시는 이 글자만 승인으로 보고 나머지는 전부 반려다. */
    private static final String CONFIRM = "confirm";

    /** DB 가 돌려주는 날짜 표기. 레거시가 {@code SimpleDateFormat} 으로 읽던 것이다. */
    private static final DateTimeFormatter STORED =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** {@code yyyy-MM-dd HH:mm:ss} 의 길이. 뒤에 소수점 이하가 붙어도 여기까지만 읽는다. */
    private static final int STORED_LENGTH = 19;

    /** 결재 도장에 찍히는 날짜 표기. */
    private static final DateTimeFormatter STAMPED = DateTimeFormatter.ofPattern("MM/dd");

    private final ProposalMapper proposalMapper;

    private final UserMapper userMapper;

    /**
     * @param proposalMapper 결재 매퍼
     * @param userMapper 사용자 매퍼. 결재자 사번을 이름으로 푼다
     */
    public ProposalService(ProposalMapper proposalMapper, UserMapper userMapper) {
        this.proposalMapper = proposalMapper;
        this.userMapper = userMapper;
    }

    /**
     * 결재 리스트 한 페이지.
     *
     * <p>조회 조건만으로는 목록이 좁혀지지 않는다. 결재선 어딘가에 내가 있기만 하면
     * 걸리므로 아직 오지 않은 차례와 이미 지나간 차례까지 딸려 온다. <b>가져온 뒤에</b>
     * «지금 내 차례인 것»만 남긴다 — 레거시가 그 자리에서 그렇게 했다(D-052).
     *
     * <p>거르기가 페이지를 자른 <b>뒤에</b> 일어나므로 한 페이지에 남는 줄 수가 들쭉날쭉
     * 하고, 페이지 수를 세는 쪽에는 이 거르기가 없다. 레거시와 같다.
     *
     * @param userId 현재 사용자 사번
     * @param result 상태 필터(3/1/2)
     * @param requestedPage 요청된 페이지
     * @return 목록과 페이징
     */
    @Transactional(readOnly = true)
    public ProposalPage list(String userId, int result, int requestedPage) {
        int total = proposalMapper.countBy(userId, result);
        Pagination pagination = Pagination.of(total, requestedPage);
        List<ProposalRow> rows = proposalMapper.findPage(
                        userId, result, pagination.start(), pagination.numPerPage())
                .stream()
                .filter(row -> row.isTurnOf(userId))
                .toList();
        return new ProposalPage(rows, pagination);
    }

    /**
     * 결재를 등록한다.
     *
     * <p>레거시는 등록 뒤 그 문서의 결재를 조회해 첫 건의 id 로 문서 상세로 보냈다.
     * 방금 만든 결재가 그것이므로 {@code LAST_INSERT_ID()} 로 읽는다.
     *
     * @param userId 기안자 사번
     * @param documentId 문서번호
     * @param routeId 결재라인 번호
     * @return 만들어진 결재 id. 실패하면 0
     */
    @Transactional
    public long create(String userId, long documentId, int routeId) {
        if (proposalMapper.insert(documentId, userId, routeId, 0) != 1) {
            return 0;
        }
        return proposalMapper.lastInsertId();
    }

    /**
     * 문서 상세 한 벌을 만든다.
     *
     * <p>결재 스탬프 표가 이 화면의 전부다. 결재선을 {@code ;} 로 쪼개 자리를 만들고,
     * 같은 문서의 결재 자취를 <b>순서대로 짝지어</b> 도장을 찍는다. 짝짓기가 사번이
     * 아니라 <b>자리 번호</b>로 이뤄지는 것이 레거시 그대로이며, 그래서 자취의 순서가
     * 곧 화면이다(D-050).
     *
     * @param proposalId 결재번호
     * @return 화면 한 벌. 결재가 없으면 {@code null}
     */
    @Transactional(readOnly = true)
    public ProposalDocument document(long proposalId) {
        ProposalView proposal = proposalMapper.findView(proposalId);
        if (proposal == null) {
            return null;
        }
        List<ProposalView> history = proposalMapper.findViewsByDocument(proposal.documentId());
        // 결재라인이 지워지면 route 가 비어 온다. 레거시는 그 자리에서 죽지만(NPE)
        // 그 죽음은 옮기지 않는다 — 자리가 없는 표를 그린다(D-041 과 같은 판단).
        String[] route = proposal.route() == null ? new String[0] : proposal.route().split(";");

        List<Integer> reviewCells = new ArrayList<>();
        for (int index = 1; index < route.length - 1; ++index) {
            reviewCells.add(index);
        }

        List<ProposalStamp> stamps = new ArrayList<>();
        for (int index = 0; index < route.length; ++index) {
            if (index >= history.size()) {
                stamps.add(new ProposalStamp(false, "", ""));
                continue;
            }
            String approvedAt = history.get(index).approvedAt();
            boolean approved = approvedAt != null;
            stamps.add(new ProposalStamp(
                    true,
                    approved ? userMapper.findUserName(route[index]) : "",
                    approved ? stampDate(approvedAt) : ""));
        }

        List<ProposalComment> comments = new ArrayList<>();
        for (ProposalView step : history) {
            String comment = step.comment() == null ? "" : step.comment();
            if (!comment.isBlank()) {
                comments.add(new ProposalComment(userMapper.findUserName(step.userId()), comment));
            }
        }

        boolean editable = proposal.result() == IN_PROGRESS || proposal.result() == PASSED;
        return new ProposalDocument(
                proposal.id(),
                proposal.subject(),
                proposal.content(),
                proposal.step(),
                editable,
                reviewCells,
                stamps,
                comments);
    }

    /**
     * 승인하거나 반려한다.
     *
     * <p>레거시 {@code proposalDocumentProc.jsp} 를 옮겼다. 갈래는 셋이다.
     *
     * <ul>
     *   <li>마지막 차례가 아닌 승인 — 내 차례를 닫고 <b>다음 차례를 만든다</b>.
     *       다음 결재자는 결재선의 {@code step + 1} 번째 사번이다
     *   <li>마지막 차례의 승인 — 내 차례를 닫고 문서의 결재 전부를 «승인» 으로 바꾼다
     *   <li>반려 — 의견만 남기고 문서의 결재 전부를 «반려» 로 바꾼다. 승인 시각은
     *       찍히지 않아 도장도 남지 않는다
     * </ul>
     *
     * <p>레거시는 이 여러 문장을 트랜잭션 없이 던졌다. 중간에 끊기면 «내 차례는 닫혔는데
     * 다음 차례가 없는» 결재가 남는다. 한 트랜잭션으로 묶는다(D-051).
     *
     * <p><b>내 차례인지 본다.</b> 레거시는 보지 않았다 — 결재번호만 맞으면 남의 차례도
     * 승인되고 반려됐다(D-049). 2단계에서 막았다(D-102): 결재선의 {@code step} 번째가
     * 누른 사람이어야 하고, 그 차례가 아직 진행중이어야 한다. 판정은 결재 리스트가
     * 이미 하던 것과 같다(D-052 의 {@code isTurnOf}).
     *
     * @param proposalId 결재번호
     * @param userId 누른 사람의 사번
     * @param result 승인이면 {@code confirm}. 그 밖의 값은 전부 반려다
     * @param comment 결재 의견
     * @return 처리 결과. 대상이 없으면 {@code NOT_FOUND}, 남의 차례면 {@code NOT_YOUR_TURN}
     */
    @Transactional
    public Decision decide(long proposalId, String userId, String result, String comment) {
        ProposalTarget target = proposalMapper.findTarget(proposalId);
        if (target == null) {
            return Decision.NOT_FOUND;
        }
        ProposalView current = proposalMapper.findView(proposalId);
        String[] line = current.route() == null ? new String[0] : current.route().split(";");
        boolean myTurn = current.result() == IN_PROGRESS
                && target.step() < line.length
                && line[target.step()].equals(userId);
        if (!myTurn) {
            return Decision.NOT_YOUR_TURN;
        }
        if (!CONFIRM.equals(result)) {
            proposalMapper.reject(proposalId, comment);
            proposalMapper.rejectAll(target.documentId());
            return Decision.done("반려하였습니다.");
        }

        ProposalLastStep last = proposalMapper.findLastStep(target.documentId());
        if (last != null && last.isFinal()) {
            proposalMapper.confirm(proposalId, comment);
            proposalMapper.confirmAll(target.documentId());
            return Decision.done("결재하였습니다.");
        }

        proposalMapper.confirm(proposalId, comment);
        int nextStep = target.step() + 1;
        proposalMapper.insert(target.documentId(), line[nextStep], target.routeId(), nextStep);
        return Decision.done("결재완료하였습니다.");
    }

    /**
     * 승인/반려의 처리 결과.
     *
     * <p>화면이 갈 곳이 셋이라 갈래도 셋이다 — 대상 없음(잘못된 접근), 남의 차례
     * (권한 없음, D-102), 처리됨(안내 문구).
     *
     * @param status 갈래
     * @param message 화면에 띄울 안내. 처리됐을 때만 있다
     */
    public record Decision(Status status, String message) {

        /** 처리 갈래. */
        public enum Status { DONE, NOT_FOUND, NOT_YOUR_TURN }

        /** 대상 결재가 없다. */
        public static final Decision NOT_FOUND = new Decision(Status.NOT_FOUND, null);

        /** 내 차례가 아니다(D-102). */
        public static final Decision NOT_YOUR_TURN =
                new Decision(Status.NOT_YOUR_TURN, null);

        private static Decision done(String message) {
            return new Decision(Status.DONE, message);
        }

        /**
         * @return 처리됐으면 {@code true}
         */
        public boolean done() {
            return status == Status.DONE;
        }
    }

    /**
     * DB 표기의 날짜를 도장 표기로 바꾼다.
     *
     * <p>레거시는 {@code SimpleDateFormat("yyyy-MM-dd hh:mm:ss")} 로 읽어 {@code MM/dd}
     * 로 찍었다. 형식 문자열의 {@code hh} 는 12시간제라 {@code 16:21:01} 같은 값과
     * 맞지 않지만, 관대한 해석이 그 값을 같은 날의 16시로 되돌려 놓아 날짜는 어긋나지
     * 않는다. 우리는 24시간제로 읽어 같은 결과를 낸다.
     *
     * <p>읽지 못하면 레거시는 예외로 화면 전체가 죽는다. 그 죽음은 옮기지 않고 원문을
     * 그대로 둔다(D-033 과 같은 판단).
     *
     * @param stored DB 가 돌려준 날짜 문자열
     * @return {@code MM/dd}
     */
    private String stampDate(String stored) {
        if (stored.length() < STORED_LENGTH) {
            return stored;
        }
        try {
            return LocalDateTime.parse(stored.substring(0, STORED_LENGTH), STORED).format(STAMPED);
        } catch (DateTimeParseException e) {
            return stored;
        }
    }

    /**
     * 결재 리스트 한 페이지.
     *
     * @param rows 이 페이지의 결재 목록
     * @param pagination 페이징 정보
     */
    public record ProposalPage(List<ProposalRow> rows, Pagination pagination) {
    }
}
