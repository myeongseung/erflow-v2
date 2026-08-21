package com.erflow.work;

/**
 * 근태 확인 표의 한 사람. {@code user_view} 한 행에서 이 화면이 쓰는 칸만.
 *
 * @param id 사번
 * @param name 성명
 * @param deptName 부서
 * @param jobName 직책
 */
public record WorkUserRow(String id, String name, String deptName, String jobName) {
}
