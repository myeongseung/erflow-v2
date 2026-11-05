package com.erflow.proposal;

import java.util.List;

/**
 * 결재라인 목록 한 줄(가공 후). 화면에 그대로 뿌린다.
 *
 * <p>{@code steps} 는 결재경로를 사람마다 푼 것이다(D-160). 화면은 이것으로 사람 알약을
 * 줄지어 그린다. {@code routeChain} 은 같은 것을 한 줄 글자로 이은 것 — 검색과
 * 시험이 쓴다.
 *
 * @param id 결재관리번호
 * @param nickname 결재라인명
 * @param routeChain 사람마다 푼 결재경로. «[부서/직급] 이름(사번) -&gt; ...»
 * @param steps 결재 순서대로의 사람들
 * @param createdAt 생성 시간. 목록이 아닌 곳(고르기 대화상자)에서는 {@code null}
 */
public record ProposalRouteListItem(
        int id, String nickname, String routeChain, List<RouteUser> steps, String createdAt) {
}
