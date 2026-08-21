package com.erflow.admin.document;

/**
 * 문서 양식 한 행. {@code template_tbl} 그대로다.
 *
 * <p>결재에 올릴 문서의 <b>빈 서식</b>이다. 내용은 HTML 이고 CKEditor 가 만든다.
 * 결재 문서 본문({@code document_tbl})과는 다른 표다 — 그쪽은 이 서식으로 쓴 실제 문서다.
 *
 * @param id 문서 번호
 * @param subject 문서 제목(양식명)
 * @param content 양식 내용. HTML 이다
 */
public record TemplateRow(int id, String subject, String content) {
}
