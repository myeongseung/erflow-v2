package com.erflow.admin.document;

import com.erflow.common.SafeHtml;

/**
 * 문서 양식 한 행. {@code template_tbl} 그대로다.
 *
 * <p>결재에 올릴 문서의 <b>빈 서식</b>이다. 내용은 HTML 이고 CKEditor 가 만든다.
 * 결재 문서 본문({@code document_tbl})과는 다른 표다 — 그쪽은 이 서식으로 쓴 실제 문서다.
 *
 * @param id 문서 번호
 * @param subject 문서 제목(양식명)
 * @param content 양식 내용. HTML 이다. 미리보기에는 걸러서 나간다 — {@link #contentSafe()}
 */
public record TemplateRow(int id, String subject, String content) {

    /**
     * 목록의 «보기» 미리보기에 찍는 양식. 거른 HTML 이다(D-118).
     *
     * <p>편집 화면({@code form-register})은 이 값을 쓰지 않는다 — 그쪽은 원본을
     * 편집기에 넣어야 하므로 {@link #content()} 를 그대로 받는다. 거른 값을 넣으면
     * 저장할 때마다 서식이 조금씩 깎인다.
     *
     * @return 거른 양식 내용
     */
    public String contentSafe() {
        return SafeHtml.clean(content);
    }
}
