package com.erflow.admin.permission;

/**
 * 메뉴 한 줄과 그 메뉴가 가리키는 프로그램 권한 (D-135). 메뉴 관리 화면의 재료다.
 *
 * <p>화면을 가리키지 않는 메뉴(그룹·로그아웃·설정)는 프로그램 쪽 값이 전부
 * {@code null} 이다. 화면은 가리키는데 권한 행이 없는 메뉴는 {@code programRowId} 가
 * {@code null} 이고 — 그 화면에는 아무도 못 들어간다.
 *
 * @param menuId 메뉴 번호
 * @param placement 표시 위치. {@code SIDE} 또는 {@code HEADER}
 * @param parentId 상위 메뉴. 최상위면 {@code null}
 * @param label 화면에 보이는 문구
 * @param visibility 표시 조건. {@code ALWAYS} 또는 {@code ADMIN}
 * @param hasScreen 화면을 가리키면 {@code true} — 권한 대상이라는 뜻
 * @param programRowId 권한 행 번호. 수정 링크에 싣는다
 * @param programName 프로그램 이름
 * @param deptLevel 들어올 수 있는 부서 비트들
 * @param jobLevel 들어올 수 있는 직급 비트들
 */
public record MenuProgramRow(
        int menuId,
        String placement,
        Integer parentId,
        String label,
        String visibility,
        boolean hasScreen,
        Integer programRowId,
        String programName,
        Long deptLevel,
        Long jobLevel) {
}
