package com.erflow.proposal;

/**
 * 결재경로를 사람마다 푼 한 사람. 결재라인 등록·수정 화면의 결재자 표 한 줄이고,
 * 목록·고르기 대화상자의 사람 알약 하나다(D-160).
 *
 * @param id 사번
 * @param name 이름
 * @param deptName 부서
 * @param jobName 직급
 */
public record RouteUser(String id, String name, String deptName, String jobName) {
}
