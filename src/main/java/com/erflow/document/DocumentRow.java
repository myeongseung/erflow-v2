package com.erflow.document;

/**
 * 문서 한 건. 레거시 {@code document_view} 한 행에 대응한다.
 *
 * <p>찾기 팝업이 쓰는 만큼만 담는다. 문서 도메인 자체는 아직 이관 전이다 —
 * 필요해지면 그때 늘린다.
 *
 * @param id 문서번호
 * @param subject 문서명
 */
public record DocumentRow(long id, String subject) {
}
