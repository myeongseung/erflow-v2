package com.erflow.post;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 게시글 매퍼. 레거시 {@code post_view} 를 그대로 쓴다. */
@Mapper
public interface PostMapper {

    /**
     * 게시글 한 페이지.
     *
     * @param boardId 게시판 번호
     * @param search 검색 조건
     * @param start 조회 시작 위치
     * @param count 가져올 건수
     * @return 게시글 목록
     */
    List<PostRow> findPage(
            @Param("boardId") int boardId,
            @Param("search") PostSearch search,
            @Param("start") int start,
            @Param("count") int count);

    /**
     * 게시글 수.
     *
     * @param boardId 게시판 번호
     * @param search 검색 조건
     * @return 건수
     */
    int countBy(@Param("boardId") int boardId, @Param("search") PostSearch search);

    /**
     * 게시글 한 건.
     *
     * @param id 글번호
     * @return 게시글. 없으면 {@code null}
     */
    PostRow findById(@Param("id") int id);

    /**
     * 게시글별 댓글 수.
     *
     * @param postId 글번호
     * @return 댓글 수
     */
    int countComments(@Param("postId") int postId);

    /**
     * 게시글에 붙은 첨부파일의 원본 이름. 목록의 첨부 아이콘 표시에만 쓴다.
     *
     * @param postId 글번호
     * @return 첨부 원본 이름. 없으면 {@code null}
     */
    String findFirstAttachmentName(@Param("postId") int postId);

    /**
     * 게시판에서 가장 큰 스레드 그룹 번호. 새 글의 {@code depth} 를 정하는 데 쓴다.
     *
     * @param boardId 게시판 번호
     * @return 최대 depth. 글이 없으면 {@code null}
     */
    Integer findMaxDepth(@Param("boardId") int boardId);

    /**
     * 새 글을 넣는다. 생성된 키가 {@code post.id} 로 되돌아온다.
     *
     * @param post 넣을 값
     * @return 반영된 행 수
     */
    int insert(@Param("post") PostWrite post);

    /**
     * 답변글을 넣는다.
     *
     * @param post 넣을 값
     * @return 반영된 행 수
     */
    int insertReply(@Param("post") PostWrite post);

    /**
     * 등록 직후 자기 자신을 스레드 뿌리로 가리키게 한다.
     *
     * @param id 글번호
     * @return 반영된 행 수
     */
    int pointToSelf(@Param("id") int id);

    /**
     * 같은 스레드에서 뒤에 오는 글들의 위치를 하나씩 민다.
     *
     * @param refId 스레드 뿌리 글번호
     * @param pos 기준 위치
     * @return 반영된 행 수
     */
    int shiftPositions(@Param("refId") int refId, @Param("pos") int pos);

    /**
     * 제목과 본문을 고친다.
     *
     * @param id 글번호
     * @param subject 제목
     * @param content 본문
     * @return 반영된 행 수
     */
    int update(@Param("id") int id,
               @Param("subject") String subject,
               @Param("content") String content);

    /**
     * 조회수를 하나 올린다.
     *
     * @param id 글번호
     * @return 반영된 행 수
     */
    int incrementCount(@Param("id") int id);

    /**
     * 이 글을 뿌리로 삼는 글 수. 자기 자신이 포함된다.
     *
     * @param id 글번호
     * @return 건수
     */
    int countInThread(@Param("id") int id);

    /**
     * 글을 지운다.
     *
     * @param id 글번호
     * @return 반영된 행 수
     */
    int delete(@Param("id") int id);

    /**
     * 답변글이 달린 글은 지우지 않고 삭제 표시만 한다.
     *
     * @param id 글번호
     * @return 반영된 행 수
     */
    int markDeleted(@Param("id") int id);
}
