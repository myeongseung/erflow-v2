package com.erflow.post;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 게시판 매퍼. 권한 판정은 하지 않고 마스크 값만 읽어 온다. */
@Mapper
public interface BoardMapper {

    /**
     * 이름으로 게시판을 찾는다. 레거시는 정렬도 페이징도 걸지 않는다.
     *
     * @param keyword 검색어. {@code null} 이거나 비면 전체
     * @return 게시판 목록
     */
    List<Board> findAll(@Param("keyword") String keyword);

    /**
     * 게시판 수를 센다.
     *
     * @param keyword 검색어
     * @return 건수
     */
    int countBy(@Param("keyword") String keyword);

    /**
     * 게시판 한 건을 읽는다.
     *
     * @param id 게시판 번호
     * @return 게시판. 없으면 {@code null}
     */
    Board findById(@Param("id") int id);
}
