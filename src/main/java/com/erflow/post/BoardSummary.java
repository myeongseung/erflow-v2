package com.erflow.post;

import java.time.LocalDateTime;

/**
 * 게시판 목록 한 줄에 붙는 값 — 글 수와 최신 글 (D-154).
 *
 * <p>한 쪽의 게시판 전부를 한 번에 읽는다. 게시판마다 따로 물으면 둘씩 는다.
 *
 * @param boardId 게시판 번호
 * @param postCount 글 수
 * @param recentPostId 최신 글 번호. 글이 없으면 {@code null}
 * @param recentSubject 최신 글 제목
 * @param recentCreatedAt 최신 글 작성 시각
 */
public record BoardSummary(
        int boardId,
        int postCount,
        Integer recentPostId,
        String recentSubject,
        LocalDateTime recentCreatedAt) {
}
