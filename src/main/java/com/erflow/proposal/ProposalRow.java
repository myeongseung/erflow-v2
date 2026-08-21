package com.erflow.proposal;

/**
 * 결재 리스트 한 줄. {@code proposal_view} 한 행 중 목록이 쓰는 칸.
 *
 * <p>{@code step} 과 {@code route} 는 화면에 찍히지 않는다. «지금 이 결재가 내 차례인가»
 * 를 가리는 데만 쓴다(D-052).
 *
 * @param id 번호
 * @param documentId 문서번호
 * @param subject 문서제목
 * @param receivedAt 생성날짜
 * @param step 결재 차례
 * @param route 결재선. 사번을 {@code ;} 로 이었다
 */
public record ProposalRow(
        long id, long documentId, String subject, String receivedAt, int step, String route) {

    /**
     * 이 결재가 그 사람의 차례인지 본다.
     *
     * <p>결재선의 {@code step} 번째가 그 사람이면 차례다. 레거시는 결재라인을 다시
     * 조회해 사번을 하나씩 객체로 만든 뒤 같은 것을 견줬다 — 컬럼을 그대로 읽는다(D-041).
     *
     * @param userId 사번
     * @return 그 사람의 차례면 {@code true}. 결재선이 비었거나 차례가 결재선 밖이면
     *     {@code false} — 레거시는 그 자리에서 죽지만 그 죽음은 옮기지 않는다
     */
    public boolean isTurnOf(String userId) {
        if (route == null) {
            return false;
        }
        String[] users = route.split(";");
        return users.length > step && users[step].equals(userId);
    }
}
