package com.erflow.admin.permission;

import java.util.List;

/**
 * 부서·직급 비트 자리 계산.
 *
 * <p>부서와 직급은 저마다 <b>비트 하나</b>를 신분증처럼 갖는다({@code level}). 프로그램은
 * 들어올 수 있는 비트들을 모아 두고, 사용자는 자기 부서·직급이 가진 비트들을 들고 온다.
 * 자세한 구조는 {@code migration/design/02-permission-bitmask.md}.
 *
 * <p><b>계산은 반드시 Java 에서 한다.</b> MariaDB 의 비트 연산 결과는 부호 없는
 * {@code BIGINT} 라, 관리자 비트(최상위)가 걸리면 판정이 뒤집힌다
 * ({@link com.erflow.auth.Permissions} 주석 참조).
 */
public final class Levels {

    private Levels() {
    }

    /**
     * 아직 아무도 쓰지 않는 가장 낮은 비트를 고른다.
     *
     * <p>레거시 {@code PermissionServiceImpl.next(flag)} 에 대응한다. 쓰이는 비트를 전부
     * {@code |} 로 모은 뒤 0인 자리를 아래에서부터 찾는다.
     *
     * <h2>레거시와 하나 다르다</h2>
     *
     * <p>레거시는 자리를 {@code 1 << i} 로 만든다. {@code 1} 이 {@code int} 라 {@code i}
     * 가 31이면 결과가 {@code Integer.MIN_VALUE} 이고, {@code long} 으로 넓히면 비트
     * 31~63 이 전부 켜진다 — <b>그 안에 관리자 비트가 있다.</b> 32번째는 첫 부서와 같은
     * 비트를 받아 남의 권한을 그대로 물려받는다. 여기서는 {@code 1L << i} 로 계산한다(D-060).
     *
     * @param used 이미 쓰이는 비트들
     * @return 빈 비트 하나. 64자리가 모두 차 있으면 0
     */
    public static long next(List<Long> used) {
        long taken = 0L;
        for (long level : used) {
            taken |= level;
        }
        for (int position = 0; position < Long.SIZE; ++position) {
            if ((taken >>> position & 1L) == 0L) {
                return 1L << position;
            }
        }
        return 0L;
    }

    /**
     * 체크된 항목들의 비트를 하나로 합친다.
     *
     * @param base 시작값. 자기 비트나 관리자 비트가 들어온다
     * @param levels 합칠 비트들
     * @return 합쳐진 비트마스크
     */
    public static long combine(long base, List<Long> levels) {
        long result = base;
        for (long level : levels) {
            result |= level;
        }
        return result;
    }

    /**
     * 그 비트가 켜져 있는지 본다. 체크박스를 그릴 때 쓴다.
     *
     * @param permission 가진 비트들
     * @param level 볼 비트
     * @return 켜져 있으면 {@code true}
     */
    public static boolean has(long permission, long level) {
        return (permission & level) != 0L;
    }
}
