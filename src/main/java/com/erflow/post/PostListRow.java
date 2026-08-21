package com.erflow.post;

/**
 * 게시글 목록 화면에 뿌리는 한 줄.
 *
 * <p>레거시는 화면 안에서 글마다 댓글 수와 첨부 유무를 다시 질의했다. 그 호출을
 * 서비스로 옮겨 담은 것이 이 레코드다.
 *
 * @param post 게시글
 * @param displayNumber 화면에 찍히는 글번호. 실제 ID 가 아니라 역순 일련번호다
 * @param commentCount 댓글 수. 0 이면 화면에 표시하지 않는다
 * @param attachmentName 첨부 원본 이름. 없으면 {@code null}
 */
public record PostListRow(
        PostRow post,
        int displayNumber,
        int commentCount,
        String attachmentName) {

    /**
     * 첨부 아이콘을 그릴지 여부.
     *
     * @return 레거시 {@code filename != null && !filename.isEmpty()} 와 같은 조건
     */
    public boolean hasAttachment() {
        return attachmentName != null && !attachmentName.isEmpty();
    }

    /**
     * 댓글 수를 표시할지 여부.
     *
     * @return 레거시 {@code cCount > 0} 와 같은 조건
     */
    public boolean hasComments() {
        return commentCount > 0;
    }
}
