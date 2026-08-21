package com.erflow.unit;

import java.time.LocalDateTime;

/**
 * 목록 화면에 뿌리는 설비 한 건.
 *
 * <p>레거시 {@code unit_view} 한 행에 대응한다. 뷰가 {@code unit_tbl} 에 관리자명과
 * 문서명을 조인해 두었고, 그 뷰를 그대로 쓴다.
 *
 * @param id 장비ID
 * @param unitName 장비명
 * @param userName 관리자명. 조인 결과가 없으면 {@code null}
 * @param documentName 문서명. 조인 결과가 없으면 {@code null}
 * @param status 장비 상태
 * @param createdAt 장비 제조일자
 */
public record UnitRow(
        String id,
        String unitName,
        String userName,
        String documentName,
        int status,
        LocalDateTime createdAt) {
}
