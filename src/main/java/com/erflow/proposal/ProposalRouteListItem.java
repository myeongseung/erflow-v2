package com.erflow.proposal;

/**
 * 결재라인 목록 한 줄(가공 후). 화면에 그대로 뿌린다.
 *
 * @param id 결재관리번호
 * @param nickname 결재라인명
 * @param routeChain 사람마다 푼 결재경로. «[부서/직급] 이름(사번) -&gt; ...»
 * @param createdAt 생성 시간
 */
public record ProposalRouteListItem(int id, String nickname, String routeChain, String createdAt) {
}
