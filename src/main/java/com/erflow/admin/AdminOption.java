package com.erflow.admin;

/**
 * 번호와 이름뿐인 콤보 항목. 직급({@code job_tbl})과 부서({@code dept_tbl})가 그렇다.
 *
 * <p>레거시는 {@code JobBean}·{@code DepartmentBean} 으로 나눠 두었지만 화면이 쓰는 것은
 * 두 칸뿐이고 쓰임도 같다. 권한 관리 화면도 같은 표를 읽는다 — 그쪽을 옮길 때 이 자리를
 * 함께 본다.
 *
 * @param id 번호. {@code option} 의 값이 된다
 * @param name 이름. 화면에 보이는 글자다
 */
public record AdminOption(int id, String name) {
}
