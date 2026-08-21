package com.erflow.task;

/**
 * 수정 화면이 읽는 수·발주 한 건. {@code task_tbl} 한 행이다.
 *
 * <p>수정 화면은 이 중 사번·문서 번호만 채워 넣고, 의뢰 시각은 비운 채 다시 받는다
 * (레거시 그대로 — {@code taskUpdate.jsp} 의 datepicker 에 값이 없다).
 *
 * @param id 의뢰 번호
 * @param userId 담당 직원 사번
 * @param companyId 협력업체 번호
 * @param documentId 문서 번호
 * @param type 0 수주 / 1 발주
 * @param taskAt 의뢰 시각
 * @param status 상태 코드
 */
public record TaskDetail(
        int id,
        String userId,
        int companyId,
        int documentId,
        int type,
        String taskAt,
        int status) {
}
