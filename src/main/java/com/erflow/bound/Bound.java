package com.erflow.bound;

/**
 * 등록할 입·출고 한 건. {@code bound_tbl} 에 넣을 값이다.
 *
 * <p>id 는 {@code AUTO_INCREMENT}, type 은 입고 0 / 출고 1 로 등록 시 정한다.
 *
 * @param productId 제품 코드({@code product_tbl_id})
 * @param userId 입고자(출고자) 사번({@code user_tbl_id})
 * @param postalCode 우편번호
 * @param address1 도로명 주소
 * @param address2 상세주소
 * @param boundedAt 입고(출고) 시간
 * @param count 수량
 * @param type 0 입고 / 1 출고
 */
public record Bound(
        String productId,
        String userId,
        String postalCode,
        String address1,
        String address2,
        String boundedAt,
        int count,
        int type) {
}
