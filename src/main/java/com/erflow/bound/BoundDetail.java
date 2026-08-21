package com.erflow.bound;

/**
 * 수정 화면이 읽는 입·출고 한 건. 레거시 {@code getBoundByType} 가 채우던 값이다.
 *
 * <h2>{@code userId} 는 사실 제품 코드다 — 레거시 결함(D-048)</h2>
 *
 * <p>레거시 {@code bound_view} 에는 {@code user_id} 컬럼이 없어, 뷰를 읽는
 * {@code extractViewBoundBean} 이 그 자리에 {@code product_id} 를 넣는다. 그래서 수정
 * 화면의 숨은 {@code userId} 에 제품 코드가 들어가고, 직원을 다시 고르지 않고 제출하면
 * {@code user_tbl_id} 가 제품 코드로 덮인다. 그대로 옮긴다 — {@code userId} 를 뷰의
 * {@code product_id} 에서 채운다.
 *
 * @param productId 제품 코드
 * @param productName 제품명
 * @param userId 레거시가 제품 코드로 채우는 «사번» (D-048)
 * @param userName 입고자(출고자) 이름
 * @param postalCode 우편번호
 * @param address1 도로명 주소
 * @param address2 상세주소
 * @param count 수량
 */
public record BoundDetail(
        String productId,
        String productName,
        String userId,
        String userName,
        String postalCode,
        String address1,
        String address2,
        int count) {
}
