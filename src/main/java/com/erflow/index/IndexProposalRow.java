package com.erflow.index;

import com.erflow.common.ProposalStatus;

/**
 * 메인 화면의 «전자결재» 위젯 한 줄. {@code recent_proposal_view} 한 행.
 *
 * <p>결재 리스트 화면과 달리 <b>«내 차례» 를 거르지 않는다</b>(D-052 참조). 내가
 * 기안한 것과 내 차례로 와 있는 것이 함께 나온다.
 *
 * @param id 결재 번호
 * @param subject 문서 제목
 * @param receivedAt 도착 시각
 * @param result 결재 상태 코드
 */
public record IndexProposalRow(long id, String subject, String receivedAt, int result) {

    /**
     * @return 결재 상태 라벨. 아는 코드가 아니면 {@code null}
     */
    public String statusLabel() {
        return ProposalStatus.label(result);
    }
}
