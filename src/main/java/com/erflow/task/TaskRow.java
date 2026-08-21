package com.erflow.task;

/**
 * 목록 화면에 뿌리는 수·발주 한 건.
 *
 * <p>레거시 {@code task_view} 한 행 중 목록이 쓰는 칸만 담는다. 뷰가
 * {@code task_tbl} 에 직원·부서·회사·문서를 조인해 두었다.
 *
 * @param id 의뢰 번호({@code task_view.task_id})
 * @param userName 담당 직원명
 * @param deptName 부서명
 * @param companyName 회사명
 * @param subject 문서 제목. 없으면 {@code null}
 * @param taskAt 의뢰 시각. 레거시가 문자열로 저장하고 그대로 찍는다
 * @param status 상태 코드. 라벨은 {@link TaskStatus} 가 푼다
 */
public record TaskRow(
        int id,
        String userName,
        String deptName,
        String companyName,
        String subject,
        String taskAt,
        int status) {

    /**
     * 화면에 찍히는 문서 제목.
     *
     * <p>문서가 딸리지 않은 수·발주가 있다. 레거시는 그 자리를 {@code <%=subject%>}
     * 로 찍어 <b>«null» 이라는 네 글자가 화면에 떴다</b>(D-089). 2단계에서 빈칸으로
     * 바꿨다(D-112). 실화면 대조에는 이 화면의 «null»→빈칸이 알려진 차이로 등록돼
     * 있다.
     *
     * @return 제목. 없으면 빈칸
     */
    public String subjectLabel() {
        return subject == null ? "" : subject;
    }

    /**
     * 화면에 찍히는 상태 글자.
     *
     * @return 레거시 {@code TaskRepository.getTaskStatusCode(status)} 와 같은 값
     */
    public String statusLabel() {
        return TaskStatus.label(status);
    }
}
