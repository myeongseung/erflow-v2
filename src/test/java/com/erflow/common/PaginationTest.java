package com.erflow.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 페이징 계산.
 *
 * <p>레거시는 이 식을 {@code unitList.jsp} 스크립틀릿 안에 두었다. 화면 안에 있으면
 * 시험할 수 없고, 틀려도 드러나지 않는다. 밖으로 꺼낸 김에 경계 조건을 고정한다.
 *
 * <p>기대값은 레거시 식을 손으로 따라가 얻은 것이다. 구현을 보고 맞춘 것이 아니다.
 */
class PaginationTest {

    @Test
    @DisplayName("첫 페이지: 15건씩, 시작 위치 0")
    void firstPage() {
        Pagination page = Pagination.of(49, 1);

        assertThat(page.nowPage()).isEqualTo(1);
        assertThat(page.start()).isZero();
        assertThat(page.numPerPage()).isEqualTo(15);
        assertThat(page.totalPage()).as("49건 / 15건 = 4페이지").isEqualTo(4);
        assertThat(page.totalBlock()).as("4페이지 / 5페이지 = 1블록").isEqualTo(1);
        assertThat(page.pageNumbers()).containsExactly(1, 2, 3, 4);
    }

    @Test
    @DisplayName("블록 경계: 6페이지는 두 번째 블록의 첫 페이지")
    void blockBoundary() {
        Pagination page = Pagination.of(200, 6);

        assertThat(page.nowBlock()).isEqualTo(2);
        assertThat(page.start()).as("(6-1) * 15").isEqualTo(75);
        assertThat(page.pageNumbers()).containsExactly(6, 7, 8, 9, 10);
        assertThat(page.hasPreviousBlock()).isTrue();
        assertThat(page.hasNextBlock()).isTrue();
        assertThat(page.previousBlockPage()).as("이전 블록 첫 페이지").isEqualTo(1);
        assertThat(page.nextBlockPage()).as("다음 블록 첫 페이지").isEqualTo(11);
    }

    @Test
    @DisplayName("마지막 블록은 전체 페이지 수에서 잘린다")
    void lastBlockIsTruncated() {
        // 100건 -> 7페이지, 블록 2개. 두 번째 블록은 6,7 두 개뿐이다.
        Pagination page = Pagination.of(100, 6);

        assertThat(page.totalPage()).isEqualTo(7);
        assertThat(page.totalBlock()).isEqualTo(2);
        assertThat(page.pageNumbers()).containsExactly(6, 7);
        assertThat(page.hasNextBlock()).isFalse();
    }

    @Test
    @DisplayName("결과가 없으면 페이지 번호도 없다")
    void emptyResult() {
        Pagination page = Pagination.of(0, 1);

        assertThat(page.totalPage()).isZero();
        assertThat(page.totalBlock()).isZero();
        assertThat(page.pageNumbers()).isEmpty();
        assertThat(page.hasPreviousBlock()).isFalse();
        assertThat(page.hasNextBlock()).isFalse();
    }

    @Test
    @DisplayName("정확히 나누어떨어지면 빈 페이지를 만들지 않는다")
    void exactMultiple() {
        assertThat(Pagination.of(30, 1).totalPage()).isEqualTo(2);
        assertThat(Pagination.of(30, 1).pageNumbers()).isEqualTo(List.of(1, 2));
    }

    @Test
    @DisplayName("0 이하 페이지는 1로 본다")
    void pageIsAtLeastOne() {
        assertThat(Pagination.of(49, 0).nowPage()).isEqualTo(1);
        assertThat(Pagination.of(49, -3).start()).isZero();
    }
}
