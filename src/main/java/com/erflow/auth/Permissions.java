package com.erflow.auth;

/**
 * 부서·직급 비트마스크 권한 판정.
 *
 * <p>출처: {@code controller/PermissionController.hasProgramPermission},
 * {@code service/implementation/PermissionServiceImpl.isAdmin}.
 *
 * <h2>판정을 SQL 로 옮기면 안 된다</h2>
 *
 * <p>MySQL/MariaDB 의 비트 연산 결과는 <b>부호 없는</b> BIGINT 다. 관리자 권한값이
 * {@link Long#MIN_VALUE}(최상위 비트) 인데, DB 에서 계산하면 이렇게 어긋난다.
 *
 * <pre>
 * MySQL : -9223372036854775808 &amp; -9223372036854775808 = 9223372036854775808
 * Java  : -9223372036854775808 &amp; -9223372036854775808 = -9223372036854775808
 * </pre>
 *
 * <p>실제로 SQL 로 관리자를 세면 0명이 나온다. 레거시는 이 판정을 Java 에서 했고,
 * 이관도 Java 에서 한다. 매퍼는 값만 읽어 오고 계산하지 않는다.
 */
public final class Permissions {

    /** 관리자를 뜻하는 비트. 부서·직급 양쪽에 켜져 있어야 한다. */
    public static final long ADMIN_BIT = Long.MIN_VALUE;

    private Permissions() {
    }

    /**
     * 관리자인지 판정한다.
     *
     * @param deptPermission 부서 권한 비트마스크
     * @param jobPermission 직급 권한 비트마스크
     * @return 부서와 직급 <b>양쪽</b> 최상위 비트가 켜져 있으면 {@code true}
     */
    public static boolean isAdmin(long deptPermission, long jobPermission) {
        return (deptPermission & ADMIN_BIT) == ADMIN_BIT
                && (jobPermission & ADMIN_BIT) == ADMIN_BIT;
    }

    /**
     * 프로그램에 접근할 수 있는지 판정한다.
     *
     * <p>부서와 직급이 <b>모두</b> 걸려야 한다. 한쪽만 맞으면 접근할 수 없다.
     *
     * @param userDept 사용자 부서 권한
     * @param userJob 사용자 직급 권한
     * @param programDept 프로그램이 요구하는 부서 권한
     * @param programJob 프로그램이 요구하는 직급 권한
     * @return 접근 가능하면 {@code true}
     */
    public static boolean hasProgramPermission(
            long userDept, long userJob, long programDept, long programJob) {
        return (programDept & userDept) != 0 && (programJob & userJob) != 0;
    }
}
