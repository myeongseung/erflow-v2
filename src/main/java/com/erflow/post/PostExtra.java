package com.erflow.post;

/**
 * 목록 한 줄에 덧붙는 값 — 댓글 수와 첫 첨부 이름 (D-152).
 *
 * <p>한 페이지의 글 전부를 한 번에 읽는다. 글마다 따로 물으면 15줄에 30번이 된다.
 *
 * @param postId 글번호
 * @param commentCount 댓글 수
 * @param attachmentName 첫 첨부의 원본 이름. 없으면 {@code null}
 */
public record PostExtra(int postId, int commentCount, String attachmentName) {
}
