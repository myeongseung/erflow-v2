package com.erflow.post;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 댓글 매퍼. */
@Mapper
public interface CommentMapper {

    /**
     * 게시글의 댓글을 레거시 정렬로 읽는다.
     *
     * @param postId 글번호
     * @return 댓글 목록
     */
    List<CommentRow> findByPost(@Param("postId") int postId);

    /**
     * 댓글 한 건.
     *
     * @param id 댓글 번호
     * @return 댓글. 없으면 {@code null}
     */
    CommentRow findById(@Param("id") int id);

    /**
     * 댓글을 등록한다. {@code depth} 는 0, {@code ref_id} 는 0 으로 넣는다.
     *
     * <p>생성된 키가 {@code write.id} 로 되돌아온다. 뒤이어 {@link #pointToSelf}
     * 로 자기 자신을 가리키게 해야 완성이다.
     *
     * @param write 넣을 값
     * @return 반영된 행 수
     */
    int insert(@Param("write") CommentWrite write);

    /**
     * 등록 직후 자기 자신을 뿌리로 가리키게 한다. 레거시가 등록 후 갱신한다.
     *
     * @param id 댓글 번호
     * @return 반영된 행 수
     */
    int pointToSelf(@Param("id") int id);

    /**
     * 답글을 등록한다. {@code depth} 는 1 이다.
     *
     * @param write 넣을 값
     * @return 반영된 행 수
     */
    int insertReply(@Param("write") CommentWrite write);

    /**
     * 댓글 내용을 고친다.
     *
     * @param id 댓글 번호
     * @param comment 내용
     * @return 반영된 행 수
     */
    int update(@Param("id") int id, @Param("comment") String comment);

    /**
     * 답글이 달렸는지 본다.
     *
     * @param id 댓글 번호
     * @return 자기 자신을 제외한 답글 수
     */
    int countReplies(@Param("id") int id);

    /**
     * 댓글을 지운다.
     *
     * @param id 댓글 번호
     * @return 반영된 행 수
     */
    int delete(@Param("id") int id);

    /**
     * 답글이 달린 댓글은 지우지 않고 내용만 바꾼다.
     *
     * @param id 댓글 번호
     * @return 반영된 행 수
     */
    int markDeleted(@Param("id") int id);

    /**
     * 게시글의 댓글을 전부 지운다. 게시글 삭제 시 먼저 부른다.
     *
     * @param postId 글번호
     * @return 반영된 행 수
     */
    int deleteByPost(@Param("postId") int postId);
}
