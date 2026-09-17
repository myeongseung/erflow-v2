package com.erflow.layout;

/**
 * 메뉴가 가리키는 화면의 요구 권한 (D-135).
 *
 * <p>화면({@code screen})이 잇는 프로그램의 권한 값이다. 권한 행이 없으면 0 으로
 * 읽힌다 — 화면 접근 판정({@code ScreenAuthorizationManager})과 같은 규칙으로,
 * 0 이면 아무도 못 들어가므로 메뉴도 아무에게도 보이지 않는다.
 *
 * @param screenId 화면 번호
 * @param deptLevel 들어올 수 있는 부서 비트들
 * @param jobLevel 들어올 수 있는 직급 비트들
 */
public record MenuAccess(int screenId, long deptLevel, long jobLevel) {
}
