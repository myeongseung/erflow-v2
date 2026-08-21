package com.erflow.common;

import java.util.List;
import java.util.stream.IntStream;

/**
 * 목록 페이징.
 *
 * <p>레거시는 이 계산을 목록 화면 스크립틀릿 안에서 했다. 화면마다 같은 식을 복사해
 * 뒀고, 틀려도 드러나지 않았다. 계산을 밖으로 꺼내 시험할 수 있게 한다.
 *
 * <p>숫자와 경계 조건은 레거시 그대로다. 페이지당 15건, 블록당 5페이지.
 * {@code unitList.jsp} 와 {@code companyList.jsp} 가 같은 상수와 같은 식을 쓴다 —
 * 목록 화면을 옮길 때마다 이것을 다시 쓰지 않는다.
 *
 * @param nowPage 현재 페이지. 1부터
 * @param totalRecord 전체 건수
 * @param totalPage 전체 페이지 수
 * @param totalBlock 전체 블록 수
 * @param nowBlock 현재 블록. 1부터
 * @param start 조회 시작 위치 (offset)
 * @param numPerPage 페이지당 건수
 * @param pagePerBlock 블록당 페이지 수
 * @param pageNumbers 현재 블록에 그릴 페이지 번호
 */
public record Pagination(
        int nowPage,
        int totalRecord,
        int totalPage,
        int totalBlock,
        int nowBlock,
        int start,
        int numPerPage,
        int pagePerBlock,
        List<Integer> pageNumbers) {

    /** 페이지당 건수. 레거시 {@code numPerPage}. */
    public static final int NUM_PER_PAGE = 15;

    /** 블록당 페이지 수. 레거시 {@code pagePerBlock}. */
    public static final int PAGE_PER_BLOCK = 5;

    /**
     * 전체 건수와 현재 페이지로 페이징을 계산한다.
     *
     * @param totalRecord 전체 건수
     * @param requestedPage 요청된 페이지. 1 미만이면 1로 본다
     * @return 계산된 페이징
     */
    public static Pagination of(int totalRecord, int requestedPage) {
        return of(totalRecord, requestedPage, NUM_PER_PAGE);
    }

    /**
     * 페이지당 건수가 15가 아닌 화면용.
     *
     * <p>근태 확인이 그렇다 — 한 사람이 두 줄을 차지해 레거시가 8명씩 끊는다.
     *
     * @param totalRecord 전체 건수
     * @param requestedPage 요청된 페이지. 1 미만이면 1로 본다
     * @param numPerPage 페이지당 건수
     * @return 계산된 페이징
     */
    public static Pagination of(int totalRecord, int requestedPage, int numPerPage) {
        int nowPage = Math.max(requestedPage, 1);
        int totalPage = (int) Math.ceil(1.0 * totalRecord / numPerPage);
        int totalBlock = (int) Math.ceil(1.0 * totalPage / PAGE_PER_BLOCK);
        int nowBlock = (int) Math.ceil(1.0 * nowPage / PAGE_PER_BLOCK);
        int start = (nowPage - 1) * numPerPage;

        int pageStart = (nowBlock - 1) * PAGE_PER_BLOCK + 1;
        int pageEnd = pageStart + PAGE_PER_BLOCK;
        pageEnd = Math.min(pageEnd, totalPage + 1);

        List<Integer> numbers = pageStart < pageEnd
                ? IntStream.range(pageStart, pageEnd).boxed().toList()
                : List.of();

        return new Pagination(nowPage, totalRecord, totalPage, totalBlock, nowBlock,
                start, numPerPage, PAGE_PER_BLOCK, numbers);
    }

    /**
     * 이전 블록으로 갈 수 있는지 여부.
     *
     * @return 현재 블록이 첫 블록이 아니면 {@code true}
     */
    public boolean hasPreviousBlock() {
        return nowBlock > 1;
    }

    /**
     * 다음 블록으로 갈 수 있는지 여부.
     *
     * @return 현재 블록이 마지막 블록이 아니면 {@code true}
     */
    public boolean hasNextBlock() {
        return nowBlock < totalBlock;
    }

    /**
     * 이전 블록의 첫 페이지 번호.
     *
     * @return 레거시 {@code block(pagePerBlock, nowBlock - 1)} 이 계산하던 값
     */
    public int previousBlockPage() {
        return PAGE_PER_BLOCK * (nowBlock - 2) + 1;
    }

    /**
     * 다음 블록의 첫 페이지 번호.
     *
     * @return 레거시 {@code block(pagePerBlock, nowBlock + 1)} 이 계산하던 값
     */
    public int nextBlockPage() {
        return PAGE_PER_BLOCK * nowBlock + 1;
    }
}
