package com.erflow.document;

/**
 * 문서 목록 한 줄. {@code document_view} 한 행 중 화면이 찍는 칸.
 *
 * <p>양식이 없으면(빈 문서로 쓴 경우) {@code templateName} 이 {@code null} 이고 화면은
 * «(빈 문서)» 로 적는다. 아직 고치지 않은 문서는 {@code updatedAt} 이 {@code null} 이고
 * «(수정하지 않음)» 이 뜬다.
 *
 * @param id 문서번호
 * @param subject 제목
 * @param templateName 양식명. 없으면 {@code null}
 * @param createdAt 생성시각
 * @param updatedAt 수정시각. 없으면 {@code null}
 * @param docStatus 문서 상태 코드
 * @param proposalStatus 결재 상태 코드
 */
public record DocumentListRow(
        long id,
        String subject,
        String templateName,
        String createdAt,
        String updatedAt,
        int docStatus,
        int proposalStatus) {

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
}
