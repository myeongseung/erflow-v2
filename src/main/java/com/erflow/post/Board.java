package com.erflow.post;

/**
 * 게시판 한 건. {@code board_tbl} 에 대응한다.
 *
 * <p>권한 마스크는 {@code Long} 이 아니라 {@code long} 이다. 레거시가 컬럼을
 * {@code NULL} 로 둘 수 있게 해두었지만, 판정은 {@code getLong} 으로 읽어 0 으로
 * 떨어뜨린다 — 0 은 어떤 사용자와도 겹치지 않으므로 "아무도 못 들어감"이 된다.
 * 그 동작을 유지한다.
 *
 * @param id 게시판 번호
 * @param subject 게시판 이름
 * @param readDeptLevel 읽기 부서 권한 마스크
 * @param readJobLevel 읽기 직급 권한 마스크
 * @param writeDeptLevel 쓰기 부서 권한 마스크
 * @param writeJobLevel 쓰기 직급 권한 마스크
 */
public record Board(
        int id,
        String subject,
        long readDeptLevel,
        long readJobLevel,
        long writeDeptLevel,
        long writeJobLevel) {
}
