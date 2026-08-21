package com.erflow.proposal;

/**
 * 결재라인 등록·수정 화면의 결재자 표 한 줄.
 *
 * @param id 사번
 * @param name 이름
 * @param jobName 직급
 */
public record RouteUser(String id, String name, String jobName) {
}
