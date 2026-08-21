package com.erflow.post;

/**
 * 게시판 접근 권한 판정.
 *
 * <p>출처: {@code controller/PermissionController.hasBoardReadPermission} 및
 * {@code hasBoardWritePermission}.
 *
 * <h2>프로그램 권한과 다른 체계다</h2>
 *
 * <p>{@link com.erflow.auth.Permissions} 는 화면(프로그램) 단위 권한이고, 이쪽은
 * 게시판마다 {@code board_tbl} 에 읽기·쓰기 마스크가 따로 박혀 있다. 게시판 목록
 * 화면에 들어가는 것과 특정 게시판을 여는 것이 별개의 판정이라는 뜻이다.
 *
 * <h2>관리자 우회가 없다</h2>
 *
 * <p>레거시는 이 두 메서드에서 {@code isAdmin} 을 보지 않는다. 관리자라도 부서·직급
 * 비트가 겹치지 않으면 못 들어간다. 편의를 위해 우회를 넣으면 레거시에서 막히던
 * 사람이 통과하게 되므로 넣지 않는다.
 *
 * <h2>판정을 SQL 로 옮기면 안 된다</h2>
 *
 * <p>{@code board_tbl} 의 마스크는 음수 long 이다(예: 공지사항 쓰기 권한이
 * {@link Long#MIN_VALUE}). MariaDB 의 비트 연산은 부호 없는 BIGINT 라 SQL 로 옮기면
 * 판정이 뒤집힌다. 매퍼는 값만 읽어 오고 계산은 여기서 한다.
 */
public final class BoardPermissions {

    private BoardPermissions() {
    }

    /**
     * 게시판을 읽을 수 있는지 판정한다.
     *
     * <p>부서와 직급이 <b>모두</b> 겹쳐야 한다. 한쪽만 맞으면 못 읽는다.
     *
     * @param userDept 사용자 부서 권한
     * @param userJob 사용자 직급 권한
     * @param board 대상 게시판. {@code null} 이면 접근 불가로 본다
     * @return 읽을 수 있으면 {@code true}
     */
    public static boolean canRead(long userDept, long userJob, Board board) {
        return board != null
                && (board.readDeptLevel() & userDept) != 0
                && (board.readJobLevel() & userJob) != 0;
    }

    /**
     * 게시판에 글을 쓸 수 있는지 판정한다.
     *
     * @param userDept 사용자 부서 권한
     * @param userJob 사용자 직급 권한
     * @param board 대상 게시판. {@code null} 이면 접근 불가로 본다
     * @return 쓸 수 있으면 {@code true}
     */
    public static boolean canWrite(long userDept, long userJob, Board board) {
        return board != null
                && (board.writeDeptLevel() & userDept) != 0
                && (board.writeJobLevel() & userJob) != 0;
    }
}
