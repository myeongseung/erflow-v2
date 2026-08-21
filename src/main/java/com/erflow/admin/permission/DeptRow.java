package com.erflow.admin.permission;

/**
 * 부서 한 행. {@code dept_tbl} 그대로다.
 *
 * <p>{@code managerId} 는 화면 어디에도 나오지 않지만 갱신 문장에 들어간다. 읽어서 그대로
 * 되돌려 놓지 않으면 부서를 고칠 때마다 부서장이 지워진다.
 *
 * @param id 부서 번호
 * @param name 부서명
 * @param managerId 부서장 사번
 * @param postalCode 우편번호
 * @param address1 도로명 주소
 * @param address2 상세 주소
 */
public record DeptRow(
        int id,
        String name,
        String managerId,
        String postalCode,
        String address1,
        String address2) {
}
