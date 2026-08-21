package com.erflow.proposal;

/**
 * 승인/반려가 손대는 결재 한 건. {@code proposal_tbl} 원본 표에서 읽는다.
 *
 * <p>뷰가 아닌 원본을 읽는 것은 레거시 {@code getProposal} 과 같다. 승인하면 이 값들을
 * 그대로 물려 다음 차례를 만들기 때문이다 — 문서와 결재라인은 같고 step 만 하나 는다.
 *
 * @param id 결재번호
 * @param documentId 문서번호
 * @param routeId 결재라인 번호
 * @param step 결재 차례
 */
public record ProposalTarget(long id, long documentId, int routeId, int step) {
}
