package com.erflow.admin.home;

import com.erflow.common.ProposalStatus;

/**
 * 대시보드 «결재» 칸 한 줄. {@code recent_proposal_view} 한 행.
 *
 * @param id 결재번호
 * @param userId 결재자 사번
 * @param subject 문서제목
 * @param receivedAt 결재요청시각
 * @param result 상태 코드
 */
public record RecentProposal(
        long id, String userId, String subject, String receivedAt, int result) {

    /** 제목을 잘라 내는 길이. 레거시가 10글자에서 자르고 «...» 을 붙인다. */
    private static final int SUBJECT_LIMIT = 10;

    /**
     * 화면에 찍히는 제목.
     *
     * @return 10글자가 넘으면 잘라 «...» 을 붙인 제목
     */
    public String shortSubject() {
        return subject != null && subject.length() > SUBJECT_LIMIT
                ? subject.substring(0, SUBJECT_LIMIT) + "..."
                : subject;
    }

    /**
     * @return 상태 라벨
     */
    public String statusLabel() {
        return ProposalStatus.label(result);
    }

    /**
     * @return 상태 칸 배경색
     */
    public String statusClass() {
        return ProposalStatus.styleClass(result);
    }
}
