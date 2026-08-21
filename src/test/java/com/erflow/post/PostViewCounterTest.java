package com.erflow.post;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 조회수 집계 규칙.
 *
 * <p>레거시의 조건이 뒤집혀 있다. 그대로 옮겼는지 여기서 못을 박는다 — 언젠가
 * "버그 같으니 고치자"는 사람이 나오면 시험이 먼저 말을 건다.
 */
class PostViewCounterTest {

    @Test
    @DisplayName("쿠키가 없으면 올린다")
    void firstEverViewIncrements() {
        assertThat(PostViewCounter.shouldIncrement(null, 42)).isTrue();
    }

    @Test
    @DisplayName("쿠키는 있는데 처음 보는 글이면 안 올린다")
    void newPostWithExistingCookieDoesNotIncrement() {
        // 중복을 막으려던 조건이 뒤집혀 있다. 새 글이 오히려 안 세어진다.
        assertThat(PostViewCounter.shouldIncrement("10_11", 42)).isFalse();
    }

    @Test
    @DisplayName("이미 본 글을 또 열면 볼 때마다 올린다")
    void seenPostIncrementsEveryTime() {
        assertThat(PostViewCounter.shouldIncrement("10_42_11", 42)).isTrue();
    }

    @Test
    @DisplayName("쿠키 값에 못 들어가는 글자를 쓰지 않는다")
    void separatorIsLegalInCookies() {
        // 레거시는 ';' 로 이었고, 그래서 이미 본 글을 다시 열면 500 이 났다.
        // 실화면 대조로 찾았다 — D-033 참조.
        assertThat(PostViewCounter.nextValue("42", 42)).doesNotContain(";");
    }

    @Test
    @DisplayName("레거시가 남긴 세미콜론 값도 읽는다")
    void legacySeparatorIsStillUnderstood() {
        assertThat(PostViewCounter.shouldIncrement("10;42;11", 42)).isTrue();
    }

    @Test
    @DisplayName("쿠키가 없으면 이 글 번호로 시작한다")
    void cookieStartsWithThisPost() {
        assertThat(PostViewCounter.nextValue(null, 42)).isEqualTo("42");
    }

    @Test
    @DisplayName("이미 있는 번호를 또 이어 붙인다")
    void cookieGrowsOnRepeatView() {
        // 레거시가 그렇게 한다. 값이 계속 길어진다.
        assertThat(PostViewCounter.nextValue("42", 42)).isEqualTo("42_42");
    }

    @Test
    @DisplayName("처음 보는 글은 쿠키에 담기지도 않는다")
    void newPostIsNotRecorded() {
        // 그래서 두 번째로 열어도 여전히 "처음 보는 글"이다. 영원히 안 세어진다.
        assertThat(PostViewCounter.nextValue("10_11", 42)).isEqualTo("10_11");
    }

    @Test
    @DisplayName("번호가 겹치는 접두사에 속지 않는다")
    void prefixIsNotAMatch() {
        assertThat(PostViewCounter.shouldIncrement("420_4", 42)).isFalse();
    }
}
