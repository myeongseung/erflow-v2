package com.erflow.document;

import com.erflow.common.ProposalStatus;

/**
 * 문서 목록 한 줄. {@code document_view} 한 행 중 화면이 찍는 칸.
 *
 * <p>양식이 없으면(빈 문서로 쓴 경우) {@code templateName} 이 {@code null} 이고 화면은
 * «(빈 문서)» 로 적는다. 아직 고치지 않은 문서는 {@code updatedAt} 이 {@code null} 이고
 * «(수정하지 않음)» 이 뜬다.
 *
 * <p>결재 상태는 그 문서의 마지막 결재 행이 든 {@code result} 다(D-160). {@code document_tbl}
 * 의 {@code proposal_status} 칸은 넣을 때 0 을 적은 뒤 아무도 고치지 않아 늘 0 이었다 —
 * 화면이 그 0 을 그대로 찍고 있었다. 결재에 올린 적이 없으면 {@code null} 이다.
 *
 * @param id 문서번호
 * @param subject 제목
 * @param templateName 양식명. 없으면 {@code null}
 * @param createdAt 생성시각
 * @param updatedAt 수정시각. 없으면 {@code null}
 * @param proposalStatus 결재 상태 코드(3 진행 중 · 1 승인 · 2 반려). 결재 전이면 {@code null}
 */
public record DocumentListRow(
        long id,
        String subject,
        String templateName,
        String createdAt,
        String updatedAt,
        Integer proposalStatus) {

    /**
     * 화면에 찍히는 양식명.
     *
     * @return 양식명. 없으면 «(빈 문서)»
     */
    public String templateLabel() {
        return templateName != null ? templateName : "(빈 문서)";
    }

    /**
     * 화면에 찍히는 수정시각.
     *
     * @return 수정시각. 없으면 «(수정하지 않음)»
     */
    public String updatedLabel() {
        return updatedAt != null ? updatedAt : "(수정하지 않음)";
    }

    /**
     * 결재에 올린 적이 없는 문서인가. 그런 문서만 «결재 올리기» 를 보여 준다.
     *
     * @return 결재 전이면 참
     */
    public boolean canPropose() {
        return proposalStatus == null;
    }

    /**
     * 결재 상태 알약의 글자.
     *
     * @return «결재 전» 또는 결재 상태 이름
     */
    public String statusLabel() {
        return proposalStatus == null ? "결재 전" : ProposalStatus.label(proposalStatus);
    }

    /**
     * 결재 상태 알약의 색 클래스.
     *
     * @return {@code is-pending} 또는 상태별 클래스
     */
    public String statusClass() {
        // 결재 전은 0 과 같은 기본색(is-pending)이다.
        return ProposalStatus.styleClass(proposalStatus == null ? 0 : proposalStatus);
    }
}
