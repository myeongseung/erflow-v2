package com.erflow.post;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 댓글 업무.
 *
 * <p>레거시는 등록·수정·삭제를 {@code comment*Proc.jsp} 넷에 나눠 두었고, 각 파일이
 * 파라미터 검사와 권한 판정을 따로 했다. 판정 규칙은 그대로 두고 자리만 모은다.
 */
@Service
public class CommentService {

    private final CommentMapper commentMapper;

    /**
     * @param commentMapper 댓글 매퍼
     */
    public CommentService(CommentMapper commentMapper) {
        this.commentMapper = commentMapper;
    }

    /**
     * 게시글의 댓글 목록.
     *
     * @param postId 글번호
     * @return 레거시 정렬({@code order by ref_id}) 그대로의 목록
     */
    @Transactional(readOnly = true)
    public List<CommentRow> list(int postId) {
        return commentMapper.findByPost(postId);
    }

    /**
     * 댓글 한 건.
     *
     * @param id 댓글 번호
     * @return 댓글. 없으면 {@code null}
     */
    @Transactional(readOnly = true)
    public CommentRow get(int id) {
        return commentMapper.findById(id);
    }

    /**
     * 댓글을 등록한다.
     *
     * <p>레거시는 {@code ref_id} 를 0 으로 넣고 생성된 키로 다시 갱신한다. 뿌리
     * 댓글이 자기 자신을 가리키는 구조라 그렇다. 두 문장을 한 트랜잭션으로 묶는다 —
     * 레거시는 묶지 않아 중간에 실패하면 {@code ref_id} 가 0 인 댓글이 남는다.
     *
     * @param postId 글번호
     * @param userId 작성자 사번
     * @param comment 내용
     * @return 등록되었으면 {@code true}
     */
    @Transactional
    public boolean register(int postId, String userId, String comment) {
        if (comment == null || comment.isBlank()) {
            return false;
        }
        CommentWrite write = new CommentWrite(postId, null, userId, comment);
        if (commentMapper.insert(write) != 1) {
            return false;
        }
        return commentMapper.pointToSelf(write.getId()) == 1;
    }

    /**
     * 답글을 등록한다.
     *
     * @param postId 글번호
     * @param refId 부모 댓글 번호
     * @param userId 작성자 사번
     * @param comment 내용
     * @return 등록되었으면 {@code true}
     */
    @Transactional
    public boolean reply(int postId, int refId, String userId, String comment) {
        if (comment == null || comment.isBlank()) {
            return false;
        }
        return commentMapper.insertReply(new CommentWrite(postId, refId, userId, comment)) == 1;
    }

    /**
     * 댓글을 고친다.
     *
     * @param id 댓글 번호
     * @param comment 내용
     * @return 고쳐졌으면 {@code true}
     */
    @Transactional
    public boolean modify(int id, String comment) {
        return comment != null && !comment.isBlank() && commentMapper.update(id, comment) == 1;
    }

    /**
     * 댓글을 지운다.
     *
     * <p>답글이 달렸으면 지우지 않고 내용만 «삭제된 댓글입니다.» 로 바꾼다. 지우면
     * 답글이 부모를 잃기 때문이다 — 레거시 {@code hasReply} 분기 그대로다.
     *
     * @param id 댓글 번호
     * @return 처리되었으면 {@code true}
     */
    @Transactional
    public boolean remove(int id) {
        return commentMapper.countReplies(id) > 0
                ? commentMapper.markDeleted(id) == 1
                : commentMapper.delete(id) == 1;
    }

    /**
     * 게시글의 댓글을 전부 지운다. 게시글 삭제 전에 부른다.
     *
     * @param postId 글번호
     * @return 지워진 건수
     */
    @Transactional
    public int removeAll(int postId) {
        return commentMapper.deleteByPost(postId);
    }
}
