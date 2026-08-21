package com.erflow.user;

/**
 * 찾기 팝업 표에 뿌리는 사용자 한 건.
 *
 * <p>레거시 {@code user_view} 한 행이다. 뷰가 {@code user_tbl} 에 부서명·직급명을
 * 조인해 두었고, 팝업은 그중 네 컬럼만 읽는다.
 *
 * @param deptName 부서명
 * @param jobName 직급명
 * @param id 사번
 * @param name 이름
 */
public record UserRow(String deptName, String jobName, String id, String name) {
}
