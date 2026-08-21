package com.erflow.task;

/**
 * 내역 모달에 뿌리는 제품 한 줄.
 *
 * <p>{@code task_history_tbl} 에 {@code product_tbl} 을 조인해 이름을 함께 읽는다.
 * 레거시는 이력마다 {@code getProduct} 를 다시 불러(N+1) 이름을 채웠는데, 결과가 같은
 * 조인으로 합쳤다 — 제품이 실제로 있는 이력만 나온다는 점도 같다(모달은 count&gt;0 로
 * 저장된 이력만 본다).
 *
 * @param productId 제품 코드
 * @param name 제품명
 * @param count 수량
 */
public record TaskHistoryRow(String productId, String name, int count) {
}
