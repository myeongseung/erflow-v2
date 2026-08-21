package com.erflow.proposal;

/**
 * {@code proposal_view} 한 행 중 문서 상세가 쓰는 칸.
 *
 * <p>레거시 {@code ViewProposalBean} 에서 이 화면이 읽는 것만 남겼다. 결재 한 건은
 * <b>결재선의 한 차례</b>다 — 결재선이 넷이면 같은 문서에 결재 행이 넷 생긴다.
 *
 * @param id 결재번호
 * @param documentId 문서번호
 * @param subject 문서제목
 * @param content 문서 내용(HTML)
 * @param userId 이 차례의 결재자 사번
 * @param step 결재 차례. 결재선 문자열의 몇 번째인가
 * @param result 상태(3 진행중 / 0 내 차례 끝 / 1 승인 / 2 반려)
 * @param comment 결재 의견
 * @param approvedAt 승인 시각. 아직이면 {@code null}
 * @param routeId 결재라인 번호
 * @param route 결재선. 사번을 {@code ;} 로 이었다
 */
public record ProposalView(
        long id,
        long documentId,
        String subject,
        String content,
        String userId,
        int step,
        int result,
        String comment,
        String approvedAt,
        int routeId,
        String route) {
}
