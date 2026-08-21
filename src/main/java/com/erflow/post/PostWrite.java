package com.erflow.post;

/**
 * 글 등록/답변에 쓰는 값 묶음.
 *
 * <p>{@code id} 는 등록 후 생성된 키를 받기 위해 가변이다. MyBatis 가
 * {@code useGeneratedKeys} 로 채운다.
 */
public final class PostWrite {

    private int id;
    private final String userId;
    private final int boardId;
    private final Integer refId;
    private final String subject;
    private final String content;
    private final int depth;
    private final int pos;

    /**
     * @param userId 작성자 사번
     * @param boardId 게시판 번호
     * @param refId 스레드 뿌리 글번호. 새 글이면 {@code null}
     * @param subject 제목
     * @param content 본문
     * @param depth 스레드 그룹 번호
     * @param pos 들여쓰기 깊이
     */
    public PostWrite(String userId, int boardId, Integer refId,
                     String subject, String content, int depth, int pos) {
        this.userId = userId;
        this.boardId = boardId;
        this.refId = refId;
        this.subject = subject;
        this.content = content;
        this.depth = depth;
        this.pos = pos;
    }

    /** @return 글번호. 등록 전에는 0 이다 */
    public int getId() {
        return id;
    }

    /** @param id MyBatis 가 채우는 생성 키 */
    public void setId(int id) {
        this.id = id;
    }

    /** @return 작성자 사번 */
    public String getUserId() {
        return userId;
    }

    /** @return 게시판 번호 */
    public int getBoardId() {
        return boardId;
    }

    /** @return 스레드 뿌리 글번호 */
    public Integer getRefId() {
        return refId;
    }

    /** @return 제목 */
    public String getSubject() {
        return subject;
    }

    /** @return 본문 */
    public String getContent() {
        return content;
    }

    /** @return 스레드 그룹 번호 */
    public int getDepth() {
        return depth;
    }

    /** @return 들여쓰기 깊이 */
    public int getPos() {
        return pos;
    }
}
