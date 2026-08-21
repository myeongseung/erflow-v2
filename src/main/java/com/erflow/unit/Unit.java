package com.erflow.unit;

/**
 * 설비 한 건. {@code unit_tbl} 에 대응한다.
 *
 * @param id 장비ID
 * @param chargerId 관리자 사번. {@code user_tbl_charger_id}
 * @param documentId 문서번호. {@code document_tbl_id}
 * @param name 장비명
 * @param status 장비 상태. 0 멈춤 / 1 가동중
 * @param createdAt 제조일자. 레거시가 문자열로 넣으므로 그대로 받는다
 */
public record Unit(
        String id,
        String chargerId,
        Long documentId,
        String name,
        int status,
        String createdAt) {
}
