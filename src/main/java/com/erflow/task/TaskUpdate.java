package com.erflow.task;

/**
 * 수정할 수·발주 본체. {@code task_tbl} 에서 바꾸는 값이다.
 *
 * <p>협력업체({@code company_tbl_id})는 없다 — 레거시 {@code taskUpdate.jsp} 에 협력업체
 * 항목이 아예 없어 수정 대상이 아니다. 제조·생성 시각도 건드리지 않는다.
 *
 * <h2>{@code type} 은 언제나 0 이다 — 레거시 결함(D-044)</h2>
 *
 * <p>레거시 {@code updateTask} 는 {@code WHERE id = ? AND type = ?} 인데,
 * {@code taskUpdateProc.jsp} 가 {@code task.setType(...)} 을 부르지 않아 {@code TaskBean}
 * 기본값 0 이 박힌다. 그래서 발주(type=1) 수정은 조건에 맞는 행이 없어 항상 실패한다.
 * 그대로 옮긴다 — 컨트롤러가 flag 와 무관하게 {@code type=0} 을 넣는다.
 *
 * @param id 의뢰 번호
 * @param userId 담당 직원 사번
 * @param documentId 문서 번호
 * @param taskAt 의뢰 시각
 * @param status 상태 코드
 * @param type WHERE 절 비교값. 레거시 결함으로 언제나 0 (D-044)
 */
public record TaskUpdate(
        int id,
        String userId,
        int documentId,
        String taskAt,
        int status,
        int type) {
}
