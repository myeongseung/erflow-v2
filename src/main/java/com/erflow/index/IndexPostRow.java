package com.erflow.index;

/**
 * 메인 화면의 «공지사항»·«자유게시판» 위젯 한 줄.
 *
 * <p>두 위젯은 같은 표를 그리지만 <b>자르는 규칙이 다르다.</b> 공지사항은 제목만 열
 * 글자에서 자르고, 자유게시판은 작성자 이름까지 세 글자에서 자른다. 두 위젯이 나란히
 * 있어 눈에 띄는데, 레거시가 그렇게 적어 두었다.
 *
 * @param number 왼쪽 끝 번호. 전체 건수에서 줄 순서를 뺀 값이라 최신 글이 가장 크다
 * @param id 글번호
 * @param boardId 게시판 번호
 * @param subject 화면에 찍히는 제목. 이미 잘려 있다
 * @param name 화면에 찍히는 작성자명. 이미 잘려 있다
 * @param userId 작성자 사번
 * @param createdAt 작성일
 * @param count 조회수
 * @param commentCount 댓글 수
 */
public record IndexPostRow(
        int number,
        int id,
        int boardId,
        String subject,
        String name,
        String userId,
        String createdAt,
        int count,
        int commentCount) {

    /**
     * 작성자 칸에 찍히는 글자.
     *
     * @return {@code 이름(사번)}
     */
    public String author() {
        return name + "(" + userId + ")";
    }
}
