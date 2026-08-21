package com.erflow.admin.board;

/**
 * 게시판 관리 목록 한 줄. {@code board_tbl} 에서 화면이 찍는 칸.
 *
 * <p>권한 네 칸은 여기 담지 않는다. 목록은 «수정» 링크만 걸고 값은 보여주지 않는다 —
 * 권한 수정 화면이 그때 다시 읽는다.
 *
 * @param id 게시판 번호
 * @param subject 게시판 이름
 * @param createdAt 최초 생성시각
 */
public record AdminBoardRow(int id, String subject, String createdAt) {

    /**
     * 지울 수 있는 게시판인지.
     *
     * <p>레거시 목록은 <b>번호가 2보다 큰</b> 게시판에만 체크박스를 그린다. 공지사항(1)과
     * 자유게시판(2)은 지우지 못하게 막아 둔 것이다.
     *
     * @return 체크박스를 그려야 하면 {@code true}
     */
    public boolean deletable() {
        return id > 2;
    }
}
