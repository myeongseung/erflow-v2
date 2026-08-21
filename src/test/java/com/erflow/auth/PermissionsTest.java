package com.erflow.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 부서·직급 비트마스크 판정.
 *
 * <p>값은 레거시 {@code permission_dept_tbl} / {@code permission_job_tbl} 의 실제
 * 데이터에서 가져왔다.
 */
class PermissionsTest {

    /** 레거시 admin 계정의 부서·직급 권한. 최상위 비트만 켜져 있다. */
    private static final long ADMIN_PERMISSION = Long.MIN_VALUE;

    @Test
    @DisplayName("관리자는 부서와 직급 양쪽 최상위 비트가 켜져 있어야 한다")
    void adminNeedsBothBits() {
        assertThat(Permissions.isAdmin(ADMIN_PERMISSION, ADMIN_PERMISSION)).isTrue();

        assertThat(Permissions.isAdmin(ADMIN_PERMISSION, 256L))
                .as("부서만 관리자면 관리자가 아니다")
                .isFalse();
        assertThat(Permissions.isAdmin(1L, ADMIN_PERMISSION))
                .as("직급만 관리자면 관리자가 아니다")
                .isFalse();
        assertThat(Permissions.isAdmin(1L, 3L)).isFalse();
    }

    @Test
    @DisplayName("SQL 로 계산하면 관리자 판정이 뒤집힌다")
    void whyNotInSql() {
        // MariaDB 의 비트 연산은 부호가 없다.
        //   MySQL : -9223372036854775808 & -9223372036854775808 = 9223372036854775808
        //   Java  : -9223372036854775808 & -9223372036854775808 = -9223372036854775808
        // 부호 있는 결과라야 최상위 비트 비교가 성립한다. 그래서 판정은 Java 에서 한다.
        assertThat(ADMIN_PERMISSION & ADMIN_PERMISSION).isEqualTo(Long.MIN_VALUE);
        assertThat(Long.toUnsignedString(ADMIN_PERMISSION & ADMIN_PERMISSION))
                .isEqualTo("9223372036854775808");
    }

    @Test
    @DisplayName("프로그램 접근은 부서와 직급이 모두 걸려야 한다")
    void programNeedsBothSides() {
        long userDept = 1L;
        long userJob = 3L;

        assertThat(Permissions.hasProgramPermission(userDept, userJob, 1L, 3L)).isTrue();
        assertThat(Permissions.hasProgramPermission(userDept, userJob, 1L, 4L))
                .as("직급이 안 걸리면 접근 불가")
                .isFalse();
        assertThat(Permissions.hasProgramPermission(userDept, userJob, 2L, 3L))
                .as("부서가 안 걸리면 접근 불가")
                .isFalse();
    }

    @Test
    @DisplayName("권한이 없는 사용자는 아무것도 열지 못한다")
    void noPermissionOpensNothing() {
        assertThat(Permissions.hasProgramPermission(0L, 0L, -1L, -1L)).isFalse();
    }

    @Test
    @DisplayName("관리자 권한은 어떤 프로그램에도 걸린다")
    void adminMatchesEveryProgram() {
        // 레거시 permission_program_tbl 의 실제 요구값 (생산 설비 관리)
        long programDept = -9223372036854775802L;
        long programJob = -9223372036854775298L;

        assertThat(Permissions.hasProgramPermission(
                        ADMIN_PERMISSION, ADMIN_PERMISSION, programDept, programJob))
                .isTrue();
    }
}
