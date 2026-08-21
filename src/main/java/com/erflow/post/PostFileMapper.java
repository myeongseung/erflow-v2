package com.erflow.post;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 게시글 첨부 매퍼. */
@Mapper
public interface PostFileMapper {

    /**
     * 게시글의 첨부 목록.
     *
     * @param postId 글번호
     * @return 첨부 목록
     */
    List<PostAttachment> findByPost(@Param("postId") int postId);

    /**
     * 저장 이름으로 첨부 한 건을 찾는다. 내려받기에 쓴다.
     *
     * @param name 저장 파일명 (UUID)
     * @return 첨부. 없으면 {@code null}
     */
    PostAttachment findByStoredName(@Param("name") String name);

    /**
     * 첨부를 등록한다.
     *
     * @param file 첨부
     * @return 반영된 행 수
     */
    int insert(@Param("file") PostAttachment file);

    /**
     * 게시글의 첨부를 전부 지운다.
     *
     * @param postId 글번호
     * @return 반영된 행 수
     */
    int deleteByPost(@Param("postId") int postId);
}
