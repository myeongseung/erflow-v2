package com.erflow.product;

/**
 * 찾기 팝업이 뿌리는 제품 한 줄.
 *
 * <p>제품 도메인은 아직 이관 전이다. 팝업이 쓰는 두 칸만 읽는다 — 레거시
 * {@code ProductBean} 에는 수량과 구분도 있지만 팝업은 보지 않는다.
 *
 * @param id 제품 코드
 * @param name 제품명
 */
public record ProductRow(String id, String name) {
}
