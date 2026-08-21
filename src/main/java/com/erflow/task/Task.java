package com.erflow.task;

/**
 * 등록할 수·발주 한 건. {@code task_tbl} 에 넣을 값이다.
 *
 * <p>id 는 {@code AUTO_INCREMENT} 라 담지 않는다. 생성된 id 는 {@code created_at}
 * 과 함께 DB 가 채운다 — 등록 뒤 {@code LAST_INSERT_ID()} 로 읽어 이력을 잇는다.
 *
 * @param userId 담당 직원 사번({@code user_tbl_id})
 * @param companyId 협력업체 번호({@code company_tbl_id})
 * @param documentId 문서 번호({@code document_tbl_id})
 * @param type 0 수주 / 1 발주
 * @param taskAt 의뢰 시각. 레거시가 문자열로 넘겨 timestamp 에 넣는다
 * @param status 상태 코드
 */
public record Task(
        String userId,
        int companyId,
        int documentId,
        int type,
        String taskAt,
        int status) {
}
