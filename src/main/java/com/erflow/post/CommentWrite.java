package com.erflow.post;

/**
 * 댓글 등록에 쓰는 값 묶음.
 *
 * <p>{@code id} 가 가변인 이유는 등록 후 생성된 키를 받아야 하기 때문이다. 레거시가
 * {@code ref_id} 를 0 으로 넣고 그 키로 다시 갱신한다 — 뿌리 댓글이 자기 자신을
 * 가리키는 구조라 두 문장이 필요하다.
 */
public final class CommentWrite {

    private int id;
    private final int postId;
    private final Integer refId;
    private final String userId;
    private final String comment;

    /**
     * @param postId 글번호
     * @param refId 부모 댓글 번호. 뿌리 댓글이면 {@code null}
     * @param userId 작성자 사번
     * @param comment 내용
     */
    public CommentWrite(int postId, Integer refId, String userId, String comment) {
        this.postId = postId;
        this.refId = refId;
        this.userId = userId;
        this.comment = comment;
    }

    /** @return 댓글 번호. 등록 전에는 0 이다 */
    public int getId() {
        return id;
    }

    /** @param id MyBatis 가 채우는 생성 키 */
    public void setId(int id) {
        this.id = id;
    }

    /** @return 글번호 */
    public int getPostId() {
        return postId;
    }

    /** @return 부모 댓글 번호 */
    public Integer getRefId() {
        return refId;
    }

    /** @return 작성자 사번 */
    public String getUserId() {
        return userId;
    }

    /** @return 내용 */
    public String getComment() {
        return comment;
    }
}
