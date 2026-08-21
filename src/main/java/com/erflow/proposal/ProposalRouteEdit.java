package com.erflow.proposal;

/**
 * 수정 화면이 읽는 결재라인. {@code proposal_route_view} 한 행 중 필요한 둘.
 *
 * @param nickname 결재라인명
 * @param route 사번을 {@code ;} 로 이은 결재 순서
 */
public record ProposalRouteEdit(String nickname, String route) {
}
