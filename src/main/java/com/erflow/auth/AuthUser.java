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
 */
public record AuthUser(
        String id,
        String name,
        String password,
        long deptPermission,
        long jobPermission) {
}
