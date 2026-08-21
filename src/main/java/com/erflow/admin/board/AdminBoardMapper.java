package com.erflow.admin.board;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 게시판 관리 매퍼.
 *
 * <p>SQL 은 {@code resources/mapper/admin/AdminBoardMapper.xml} 에 있다. 게시판을 읽는
 * 매퍼가 하나 더 있다({@code com.erflow.post.BoardMapper}) — 그쪽은 사용자 화면이 쓰고
 * 권한 네 칸을 읽어 온다. 여기는 관리 화면이 쓰고 목록·생성·수정·삭제를 맡는다.
 */
@Mapper
public interface AdminBoardMapper {

    /**
     * 게시판 목록 한 페이지.
     *
     * @param keyword 게시판 이름 검색어
     * @param start 조회 시작 위치
     * @param count 가져올 건수
     * @return 게시판 목록
     */
    List<AdminBoardRow> findPage(
            @Param("keyword") String keyword,
            @Param("start") int start,
            @Param("count") int count);

    /**
     * 조건에 걸리는 게시판 수.
     *
     * @param keyword 게시판 이름 검색어
     * @return 건수
     */
    int countBy(@Param("keyword") String keyword);

    /**
     * 같은 이름의 게시판 수.
     *
     * @param subject 게시판 이름
     * @return 건수
     */
    int countByName(@Param("subject") String subject);

    /**
     * 게시판을 만든다. 권한 네 칸이 같은 값으로 들어간다.
     *
     * @param subject 게시판 이름
     * @param level 권한 네 칸에 넣을 값. 레거시는 관리자 비트다
     * @return 반영된 행 수
     */
    int insertBoard(@Param("subject") String subject, @Param("level") long level);

    /**
     * 게시판 이름을 고친다.
     *
     * @param id 게시판 번호
     * @param subject 새 이름
     * @return 반영된 행 수
     */
    int updateBoard(@Param("id") int id, @Param("subject") String subject);

    /**
     * 그 게시판에 달린 글번호.
     *
     * @param boardId 게시판 번호
     * @return 글번호 목록
     */
    List<Integer> findPostIds(@Param("boardId") int boardId);

    /**
     * 게시판을 지운다.
     *
     * @param id 게시판 번호
     * @return 반영된 행 수
     */
    int deleteBoard(@Param("id") int id);

    /**
     * 읽기 권한을 바꾼다. 부서·직급 두 칸을 함께 쓴다.
     *
     * @param boardId 게시판 번호
     * @param deptLevel 부서 비트마스크
     * @param jobLevel 직급 비트마스크
     * @return 반영된 행 수
     */
    int updateReadPermission(
            @Param("boardId") int boardId,
            @Param("deptLevel") long deptLevel,
            @Param("jobLevel") long jobLevel);

    /**
     * 쓰기 권한을 바꾼다.
     *
     * @param boardId 게시판 번호
     * @param deptLevel 부서 비트마스크
     * @param jobLevel 직급 비트마스크
     * @return 반영된 행 수
     */
    int updateWritePermission(
            @Param("boardId") int boardId,
            @Param("deptLevel") long deptLevel,
            @Param("jobLevel") long jobLevel);
}
