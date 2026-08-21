package com.erflow.post;

import java.time.LocalDateTime;

/**
 * 댓글 한 건. 레거시 {@code comment_view} 한 행에 대응한다.
 *
 * <p>계층은 두 단계뿐이다 — {@code depth} 가 0(댓글) 아니면 1(답글)이다. 실데이터로
 * 확인했다. 뿌리 댓글은 {@code refId} 가 자기 자신이고, 답글은 부모 댓글 번호다.
 *
 * @param id 댓글 번호
 * @param userId 작성자 사번
 * @param userName 작성자명
 * @param postId 글번호
 * @param refId 뿌리 댓글 번호
 * @param comment 내용
 * @param depth 0 댓글 / 1 답글
 * @param createdAt 작성일
 */
public record CommentRow(
        int id,
        String userId,
        String userName,
        int postId,
        Integer refId,
        String comment,
        int depth,
        LocalDateTime createdAt) {

    /**
     * 들여쓰기 픽셀. 레거시 {@code depth > 0 ? 30 : 0} 그대로다.
     *
     * @return 왼쪽 여백 픽셀
     */
    public int indent() {
        return depth > 0 ? 30 : 0;
    }

    /**
     * 답글을 달 수 있는지 여부.
     *
     * <p>레거시는 {@code depth == 0} 일 때만 "답글달기"를 그렸다. 답글에 답글을
     * 달 수는 없다.
     *
     * @return 뿌리 댓글이면 {@code true}
     */
    public boolean replyable() {
        return depth == 0;
    }
}
