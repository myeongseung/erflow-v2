package com.erflow.admin.permission;

import static org.assertj.core.api.Assertions.assertThat;

import com.erflow.auth.Permissions;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 비트 자리 계산. DB 없이 도는 단위 시험이다.
 *
 * <p>이 계산이 틀리면 «없던 권한이 생기는» 쪽으로 틀린다. 화면으로는 보이지 않으므로
 * 여기서 못을 박는다.
 */
class LevelsTest {

    @Test
    @DisplayName("가장 낮은 빈 자리를 준다")
    void nextGivesLowestFreeBit() {
        // 지금 데이터와 같은 모양 — 부서 8개(비트 0~7) + 관리자(비트 63).
        List<Long> used = List.of(1L, 2L, 4L, 8L, 16L, 32L, 64L, 128L, Long.MIN_VALUE);

        assertThat(Levels.next(used)).isEqualTo(256L);
    }

    @Test
    @DisplayName("중간이 비면 그 자리를 다시 쓴다")
    void nextReusesAGap() {
        // 비트 1이 비어 있다(부서를 하나 지운 뒤와 같은 모양).
        assertThat(Levels.next(List.of(1L, 4L, 8L, Long.MIN_VALUE))).isEqualTo(2L);
    }

    @Test
    @DisplayName("31번째 자리에서도 관리자 비트를 만들지 않는다")
    void nextNeverCreatesAnAdmin() {
        // 레거시는 `1 << i` 로 int 연산을 해서, 이 자리에서 비트 31~63 이 켜진 값을
        // 돌려준다 — 그 안에 관리자 비트가 있다(D-060).
        List<Long> used = List.of((1L << 31) - 1, Long.MIN_VALUE);

        long allocated = Levels.next(used);

        assertThat(allocated).isEqualTo(1L << 31);
        assertThat(Permissions.isAdmin(allocated, allocated)).isFalse();
        assertThat(allocated & Permissions.ADMIN_BIT).isZero();
    }

    @Test
    @DisplayName("32번째 자리에서도 남의 비트를 주지 않는다")
    void nextDoesNotWrapAround() {
        // 레거시는 여기서 1(첫 부서의 비트)을 돌려준다 — 새 부서가 남의 권한을
        // 그대로 물려받는다.
        List<Long> used = List.of((1L << 32) - 1, Long.MIN_VALUE);

        assertThat(Levels.next(used)).isEqualTo(1L << 32);
    }

    @Test
    @DisplayName("자리가 다 차면 0 을 준다")
    void nextReturnsZeroWhenFull() {
        assertThat(Levels.next(List.of(-1L))).isZero();
    }

    @Test
    @DisplayName("합치기는 시작값을 지우지 않는다")
    void combineKeepsBase() {
        assertThat(Levels.combine(1L, List.of(2L, 4L))).isEqualTo(7L);
        assertThat(Levels.combine(1L, List.of())).isEqualTo(1L);
        // 프로그램 권한은 관리자 비트로 시작한다. 체크를 다 지워도 남는다.
        assertThat(Levels.combine(Permissions.ADMIN_BIT, List.of())).isEqualTo(Long.MIN_VALUE);
    }

    @Test
    @DisplayName("관리자 비트가 걸려도 판정이 뒤집히지 않는다")
    void hasWorksOnTheSignBit() {
        // DB 에서 계산하면 이 자리에서 부호가 없어져 판정이 뒤집힌다.
        assertThat(Levels.has(Long.MIN_VALUE, Long.MIN_VALUE)).isTrue();
        assertThat(Levels.has(Long.MIN_VALUE | 1L, 1L)).isTrue();
        assertThat(Levels.has(1L, Long.MIN_VALUE)).isFalse();
    }
}
