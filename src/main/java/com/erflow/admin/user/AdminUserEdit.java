package com.erflow.admin.user;

/**
 * 사원을 넣거나 고칠 때 서버로 넘어오는 값.
 *
 * <p>레거시 {@code *Proc.jsp} 가 {@code UserBean} 을 채우던 자리에 해당한다. 레거시는
 * 화면이 보낸 값 중 주소 두 줄(등록)과 휴대 전화(등록·수정)를 버렸다(D-057) —
 * 2단계에서 전부 받아 저장한다(D-104).
 *
 * @param id 사번
 * @param name 이름
 * @param socialNumber 주민등록번호. 수정에서는 쓰지 않는다
 * @param email 이메일
 * @param postalCode 우편번호
 * @param address1 도로명 주소
 * @param address2 상세 주소
 * @param jobId 직급 번호
 * @param deptId 부서 번호
 * @param extensionPhone 내선 번호
 * @param mobilePhone 휴대 전화
 */
public record AdminUserEdit(
        String id,
        String name,
        String socialNumber,
        String email,
        String postalCode,
        String address1,
        String address2,
        int jobId,
        int deptId,
        String extensionPhone,
        String mobilePhone) {
}
