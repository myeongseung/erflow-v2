package com.erflow.post;

import java.time.LocalDateTime;

/**
 * 게시판 목록 화면에 뿌리는 한 줄.
 *
 * <p>레거시는 게시판마다 {@code getTotalCount} 와 {@code getPostViews(.., 0, 1)} 을
 * 화면 안에서 다시 호출해 글 수와 최신글을 채웠다. 게시판 수만큼 질의가 늘어나는
 * 구조인데, 결과가 같아야 하므로 질의 자체는 그대로 두고 호출만 서비스로 옮겼다.
 *
 * @param id 게시판 번호
 * @param subject 게시판 이름
 * @param postCount 게시글 수
 * @param recentPostId 최신글 번호. 글이 없으면 {@code null}
 * @param recentSubject 최신글 제목. 글이 없으면 {@code null}
 * @param recentCreatedAt 최신글 작성일. 글이 없으면 {@code null}
 */
public record BoardRow(
        int id,
        String subject,
        int postCount,
        Integer recentPostId,
        String recentSubject,
        LocalDateTime recentCreatedAt) {

    /**
     * 최신글이 있는지 여부.
     *
     * @return 레거시 {@code hasRecent} 와 같은 값
     */
    public boolean hasRecent() {
        return recentPostId != null;
    }
}
