package com.erflow.proposal;

/**
 * 결재라인 목록 한 줄(가공 전). {@code proposal_route_view} 한 행이다.
 *
 * <p>{@code route} 는 사번을 {@code ;} 로 이은 문자열이다. 화면은 이것을 사람마다 풀어
 * «[부서/직급] 이름(사번)» 을 {@code ->} 로 이어 보여준다 — 그 가공은
 * {@link ProposalRouteService} 가 한다.
 *
 * @param id 결재관리번호
 * @param nickname 결재라인명
 * @param route 사번을 {@code ;} 로 이은 결재 순서
 * @param createdAt 생성 시간
 */
public record ProposalRouteListRow(int id, String nickname, String route, String createdAt) {
}
