package com.erflow.auth;

/**
 * 시험용 인증 주체.
 *
 * <p>권한값은 레거시 {@code permission_dept_tbl} / {@code permission_job_tbl} 의 실제
 * 데이터를 그대로 쓴다. 지어낸 값으로 시험하면 비트마스크가 틀려도 통과한다.
 */
public final class TestUsers {

    private TestUsers() {
    }

    /**
     * 관리자.
     *
     * <p>레거시 {@code admin} 계정과 같은 권한 — 부서·직급 양쪽 최상위 비트.
     *
     * @return 모든 화면을 볼 수 있는 사용자
     */
    public static ErflowUserDetails admin() {
        return new ErflowUserDetails(
                new AuthUser("admin", "관리자", "(시험용)", Long.MIN_VALUE, Long.MIN_VALUE),
                false);
    }

    /**
     * 권한이 전혀 없는 사용자.
     *
     * @return 어떤 화면도 볼 수 없는 사용자
     */
    public static ErflowUserDetails noPermission() {
        return new ErflowUserDetails(
                new AuthUser("nobody", "무권한", "(시험용)", 0L, 0L), false);
    }

    /**
     * 비밀번호를 바꿔야 하는 사용자.
     *
     * <p>레거시는 비밀번호가 사번과 같으면 로그인시키지 않고 변경 화면으로 보냈다.
     *
     * @return 비밀번호 변경만 할 수 있는 사용자
     */
    public static ErflowUserDetails passwordChangeRequired() {
        return new ErflowUserDetails(
                new AuthUser("newbie", "신규", "(시험용)", Long.MIN_VALUE, Long.MIN_VALUE),
                true);
    }
}
