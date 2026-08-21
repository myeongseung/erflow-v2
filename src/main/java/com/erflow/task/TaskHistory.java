package com.erflow.task;

/**
 * 수·발주에 딸린 제품 한 줄. {@code task_history_tbl} 한 행이다.
 *
 * @param taskId 수·발주 번호({@code task_tbl_id})
 * @param productId 제품 코드({@code product_tbl_id})
 * @param count 수량
 */
public record TaskHistory(int taskId, String productId, int count) {
}
