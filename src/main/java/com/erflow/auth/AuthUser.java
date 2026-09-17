package com.erflow.auth;

/**
 * 인증에 필요한 사용자 정보.
 *
 * <p>레거시는 {@code select * from user_tbl where id = ?} 로 읽은 뒤 부서·직급 권한을
 * 따로 조회했다. 한 번에 읽되 값은 그대로 가져온다 — 비트마스크 계산은 DB 에서 하면
 * 안 된다({@link Permissions} 참조).
 *
 * @param id 사번
 * @param name 이름
 * @param password 저장된 비밀번호 해시
 * @param deptPermission 부서 권한 비트마스크
 * @param jobPermission 직급 권한 비트마스크
 * @param passwordChangeRequired 관리자가 초기화해 변경이 강제된 상태면 {@code true} (D-134).
 *     저장된 플래그 그대로다 — «비밀번호가 사번과 같다» 는 판정과 합치는 곳은
 *     {@link ErflowUserDetailsService} 다
 */
public record AuthUser(
        String id,
        String name,
        String password,
        long deptPermission,
        long jobPermission,
        boolean passwordChangeRequired) {
}
