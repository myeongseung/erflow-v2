package com.erflow.product;

/**
 * 제품 목록 한 줄. {@code product_tbl} 한 행이다.
 *
 * <p>찾기 팝업이 쓰는 {@link ProductRow} 와 다르다 — 그쪽은 번호와 이름만 읽는다.
 *
 * @param id 제품 ID
 * @param name 이름
 * @param count 수량
 * @param type 분류 코드
 */
public record ProductListRow(String id, String name, int count, int type) {
}
