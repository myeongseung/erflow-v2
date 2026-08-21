package com.erflow.proposal;

/**
 * 찾기 팝업이 뿌리는 결재라인 한 줄.
 *
 * <p>결재 도메인은 아직 이관 전이다. 팝업이 쓰는 세 칸만 읽는다.
 *
 * @param id 결재라인 번호
 * @param nickname 결재라인 이름. 화면에 보이는 글자이고 검색도 이것만 훑는다
 * @param route 결재 순서대로 이은 사번. 구분자는 {@code ;} 다
 */
public record ProposalRouteRow(int id, String nickname, String route) {
}
