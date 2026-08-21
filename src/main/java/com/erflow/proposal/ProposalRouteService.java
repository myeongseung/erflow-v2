package com.erflow.proposal;

import com.erflow.common.Pagination;
import com.erflow.user.UserMapper;
import com.erflow.user.UserRow;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결재라인 관리 업무.
 *
 * <p>레거시는 페이징 계산을 {@code proposalRouteList.jsp} 스크립틀릿에, DB 접근을
 * {@code ProposalRouteServiceImpl} 에 두었다. 결재경로 문자열을 사람마다 푸는 가공도
 * 화면에서 했다 — 여기로 모은다.
 */
@Service
public class ProposalRouteService {

    private final ProposalRouteMapper proposalRouteMapper;
    private final UserMapper userMapper;

    /**
     * @param proposalRouteMapper 결재라인 매퍼
     * @param userMapper 사용자 조회(결재경로 사람 풀이용)
     */
    public ProposalRouteService(ProposalRouteMapper proposalRouteMapper, UserMapper userMapper) {
        this.proposalRouteMapper = proposalRouteMapper;
        this.userMapper = userMapper;
    }

    /**
     * 내 결재라인 한 페이지. 결재경로를 사람마다 풀어 돌려준다.
     *
     * @param userId 사번
     * @param search 검색 조건
     * @param requestedPage 요청된 페이지
     * @return 목록과 페이징
     */
    @Transactional(readOnly = true)
    public RoutePage list(String userId, ProposalRouteSearch search, int requestedPage) {
        int total = proposalRouteMapper.countBy(userId, search);
        Pagination pagination = Pagination.of(total, requestedPage);
        List<ProposalRouteListRow> rows = proposalRouteMapper.findPage(
                userId, search, pagination.start(), pagination.numPerPage());

        List<ProposalRouteListItem> items = new ArrayList<>();
        for (ProposalRouteListRow row : rows) {
            items.add(new ProposalRouteListItem(
                    row.id(), row.nickname(), routeChain(row.route()), row.createdAt()));
        }
        return new RoutePage(items, pagination);
    }

    /**
     * 사번을 {@code ;} 로 이은 결재경로를 «[부서/직급] 이름(사번) -&gt; ...» 로 푼다.
     *
     * <p>레거시가 화면에서 사번마다 {@code getUserView} 를 불러 만들던 문자열이다.
     * 사번이 사라진 사용자를 가리키면 레거시는 {@code null} 에서 예외로 죽었다 — 여기서는
     * 그 사번만 «(사번)» 으로 두어 화면이 뜨게 한다.
     *
     * @param route 사번을 {@code ;} 로 이은 문자열
     * @return 사람마다 푼 결재경로
     */
    private String routeChain(String route) {
        if (route == null || route.isBlank()) {
            return "";
        }
        return java.util.Arrays.stream(route.split(";"))
                .map(sabun -> {
                    UserRow user = userMapper.findUserView(sabun);
                    return user == null
                            ? "(" + sabun + ")"
                            : "[" + user.deptName() + "/" + user.jobName() + "] "
                                    + user.name() + "(" + user.id() + ")";
                })
                .collect(Collectors.joining(" -> "));
    }

    /**
     * 등록 화면이 쓸 결재자 표를 만든다.
     *
     * <p>사용자 찾기가 고른 사번을 화면 스크립트가 Base64(URL 인코딩)로 감싸 다시
     * 넘긴다. 그것을 풀어 사람마다 조회한다. 값이 없으면 빈 표다.
     *
     * @param receiver Base64(URL 인코딩)로 감싼 {@code ;} 이은 사번. 없으면 {@code null}
     * @return 결재자 목록. 이름을 이은 문자열도 함께
     */
    @Transactional(readOnly = true)
    public RouteForm registerForm(String receiver) {
        if (receiver == null || receiver.isBlank()) {
            return new RouteForm("", List.of());
        }
        String decoded = java.net.URLDecoder.decode(
                new String(java.util.Base64.getDecoder().decode(receiver),
                        java.nio.charset.StandardCharsets.UTF_8),
                java.nio.charset.StandardCharsets.UTF_8);
        List<RouteUser> users = resolveUsers(decoded);
        // 레거시는 이름 뒤에 공백을 붙여 이었다("홍길동 김철수 ").
        StringBuilder names = new StringBuilder();
        for (RouteUser user : users) {
            names.append(user.name()).append(" ");
        }
        return new RouteForm(names.toString(), users);
    }

    /**
     * 수정 화면이 쓸 결재자 표를 만든다.
     *
     * @param id 결재관리번호
     * @return 결재라인명과 결재자 목록. 없으면 {@code null}
     */
    @Transactional(readOnly = true)
    public RouteEditForm updateForm(int id) {
        ProposalRouteEdit edit = proposalRouteMapper.findForUpdate(id);
        if (edit == null) {
            return null;
        }
        return new RouteEditForm(edit.nickname(), resolveUsers(edit.route()));
    }

    /**
     * 결재라인을 등록한다.
     *
     * <p>레거시는 결재 순서 맨 앞에 <b>만든 사람 자신</b>을 넣고 고른 결재자들을 이어
     * 붙였다. 그대로 옮긴다.
     *
     * @param userId 만든 사람 사번
     * @param nickname 결재라인명
     * @param routeIds 고른 결재자 사번(순서대로)
     * @return 들어갔으면 {@code true}
     */
    @Transactional
    public boolean create(String userId, String nickname, List<String> routeIds) {
        String route = userId + ";" + String.join(";", routeIds);
        return proposalRouteMapper.insertRoute(userId, nickname, route) == 1;
    }

    /**
     * 결재라인을 수정한다.
     *
     * <p>등록과 달리 만든 사람을 앞에 다시 넣지 않는다 — 표에 이미 들어 있는 순서를
     * 그대로 저장한다(레거시 그대로).
     *
     * @param id 결재관리번호
     * @param nickname 결재라인명
     * @param routeIds 결재자 사번(순서대로)
     * @return 수정됐으면 {@code true}
     */
    @Transactional
    public boolean update(int id, String nickname, List<String> routeIds) {
        return proposalRouteMapper.updateRoute(id, nickname, String.join(";", routeIds)) == 1;
    }

    /**
     * 선택된 결재라인을 지운다.
     *
     * @param ids 지울 결재관리번호 목록
     * @return 전부 지워졌으면 {@code true}
     */
    @Transactional
    public boolean delete(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        boolean all = true;
        for (int id : ids) {
            all &= proposalRouteMapper.deleteById(id) == 1;
        }
        return all;
    }

    private List<RouteUser> resolveUsers(String route) {
        List<RouteUser> users = new ArrayList<>();
        if (route == null || route.isBlank()) {
            return users;
        }
        for (String sabun : route.split(";")) {
            UserRow user = userMapper.findUserView(sabun);
            users.add(user == null
                    ? new RouteUser(sabun, "", "")
                    : new RouteUser(user.id(), user.name(), user.jobName()));
        }
        return users;
    }

    /**
     * 결재라인 목록 한 페이지.
     *
     * @param items 이 페이지의 결재라인(가공 후)
     * @param pagination 페이징 정보
     */
    public record RoutePage(List<ProposalRouteListItem> items, Pagination pagination) {
    }

    /**
     * 등록 화면 표.
     *
     * @param routeUserNames 이름을 이은 문자열
     * @param users 결재자 목록
     */
    public record RouteForm(String routeUserNames, List<RouteUser> users) {
    }

    /**
     * 수정 화면 표.
     *
     * @param nickname 결재라인명
     * @param users 결재자 목록
     */
    public record RouteEditForm(String nickname, List<RouteUser> users) {
    }
}
