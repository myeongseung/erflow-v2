package com.erflow.admin.permission;

/**
 * 권한 프로그램 한 행. {@code permission_program_tbl} 그대로다.
 *
 * <p>«프로그램» 은 화면 묶음의 이름이다. 레거시가 JSP 마다 {@code PROGRAM_CODE} 로
 * 박아 둔 SHA256 값이 {@code programId} 이고, 우리 앱은 {@code screen} 표가 경로를 그
 * 값에 잇는다.
 *
 * <p>권한 값은 <b>이 표</b>가 정본이다. 생성물인 {@code program} 표에는 두지 않는다(D-063).
 *
 * @param id 행 번호. 화면이 «번호» 로 찍고 수정 링크에도 싣는다
 * @param programId 프로그램 코드(SHA256)
 * @param programName 프로그램 이름
 * @param deptLevel 들어올 수 있는 부서 비트들
 * @param jobLevel 들어올 수 있는 직급 비트들
 */
public record ProgramRow(
        int id, String programId, String programName, long deptLevel, long jobLevel) {
}
