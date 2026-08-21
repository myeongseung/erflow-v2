package com.erflow.post;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 게시글 검색 조건. 레거시의 분기 불일치를 그대로 재현하는지 본다. */
class PostSearchTest {

    @Test
    @DisplayName("화이트리스트 밖 검색 항목은 조건을 만들지 않는다")
    void unknownFieldFallsBackToAll() {
        PostSearch injected = new PostSearch("subject; DROP TABLE post_tbl", "x");

        assertThat(injected.activeForList()).isFalse();
        assertThat(injected.activeForCount()).isFalse();
        assertThat(injected.column()).isNull();
    }

    @Test
    @DisplayName("목록과 개수가 서로 다른 조건으로 분기한다")
    void listAndCountDisagree() {
        // 레거시 결함이다. getPostViews 는 keyfield 를, getTotalCount 는 keyword 를
        // 본다. 검색 항목만 고르고 검색어를 비우면 갈린다. D-024 참조.
        PostSearch fieldOnly = new PostSearch("subject", "");

        assertThat(fieldOnly.activeForList()).isTrue();
        assertThat(fieldOnly.activeForCount()).isFalse();
    }

    @Test
    @DisplayName("검색어가 있으면 양쪽 다 조건을 건다")
    void bothAgreeWhenKeywordPresent() {
        PostSearch search = new PostSearch("author", "홍길동");

        assertThat(search.activeForList()).isTrue();
        assertThat(search.activeForCount()).isTrue();
        assertThat(search.column()).isEqualTo("name");
        assertThat(search.pattern()).isEqualTo("%홍길동%");
    }

    @Test
    @DisplayName("조건 없는 전체 조회")
    void none() {
        assertThat(PostSearch.none().activeForList()).isFalse();
        assertThat(PostSearch.none().activeForCount()).isFalse();
    }
}
