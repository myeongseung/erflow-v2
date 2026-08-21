package com.erflow.post;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 게시판 권한 판정.
 *
 * <p>DB 를 타지 않는다. 비트 연산이 맞는지만 본다 — 실제 데이터로 확인한 마스크
 * 값을 그대로 넣는다.
 */
class BoardPermissionsTest {

    /** 공지사항 쓰기 권한. 실데이터에서 확인한 값이다. 최상위 비트만 켜져 있다. */
    private static final long ADMIN_ONLY = Long.MIN_VALUE;

    /** 모든 비트가 켜진 마스크. 공지사항 읽기 권한이 이 값이다. */
    private static final long EVERYONE = -1L;

    private static Board board(long readDept, long readJob, long writeDept, long writeJob) {
        return new Board(1, "공지사항", readDept, readJob, writeDept, writeJob);
    }

    @Test
    @DisplayName("부서와 직급이 모두 겹쳐야 읽을 수 있다")
    void bothMustOverlap() {
        Board any = board(0b0110, 0b0110, 0, 0);

        assertThat(BoardPermissions.canRead(0b0100, 0b0010, any)).isTrue();
        assertThat(BoardPermissions.canRead(0b0100, 0b1000, any)).isFalse();
        assertThat(BoardPermissions.canRead(0b1000, 0b0010, any)).isFalse();
    }

    @Test
    @DisplayName("최상위 비트가 SQL 로 옮기면 뒤집히는 자리다")
    void topBitSurvivesInJava() {
        Board notice = board(EVERYONE, EVERYONE, ADMIN_ONLY, ADMIN_ONLY);

        // MariaDB 의 비트 연산은 부호 없는 BIGINT 라 이 판정을 SQL 로 옮기면
        // 결과가 달라진다. Java 에서 계산해야 하는 이유다.
        assertThat(ADMIN_ONLY & ADMIN_ONLY).isEqualTo(Long.MIN_VALUE);
        assertThat(BoardPermissions.canWrite(ADMIN_ONLY, ADMIN_ONLY, notice)).isTrue();
        assertThat(BoardPermissions.canWrite(EVERYONE, EVERYONE, notice)).isTrue();
        assertThat(BoardPermissions.canWrite(0b0110, 0b0110, notice)).isFalse();
    }

    @Test
    @DisplayName("관리자라고 우회하지 않는다")
    void noAdminBypass() {
        // 레거시 hasBoardReadPermission 은 isAdmin 을 보지 않는다. 편의로 우회를
        // 넣으면 레거시에서 막히던 사람이 통과한다.
        Board closed = board(0b0001, 0b0001, 0b0001, 0b0001);

        assertThat(BoardPermissions.canRead(ADMIN_ONLY, ADMIN_ONLY, closed)).isFalse();
        assertThat(BoardPermissions.canWrite(ADMIN_ONLY, ADMIN_ONLY, closed)).isFalse();
    }

    @Test
    @DisplayName("없는 게시판은 접근 불가로 본다")
    void missingBoardIsClosed() {
        assertThat(BoardPermissions.canRead(EVERYONE, EVERYONE, null)).isFalse();
        assertThat(BoardPermissions.canWrite(EVERYONE, EVERYONE, null)).isFalse();
    }

    @Test
    @DisplayName("마스크가 0 이면 아무도 못 들어간다")
    void zeroMaskLocksEveryoneOut() {
        Board unset = board(0, 0, 0, 0);

        assertThat(BoardPermissions.canRead(EVERYONE, EVERYONE, unset)).isFalse();
    }
}
