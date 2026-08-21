package com.erflow.bound;

/**
 * 수정할 입·출고 본체. {@code bound_tbl} 에서 바꾸는 값이다.
 *
 * <p>입고 시간({@code bounded_at})은 바꾸지 않는다 — 레거시 update SQL 에 없다. 화면의
 * 날짜 입력도 비어 있어 제출되지만 무시된다.
 *
 * @param id 번호
 * @param productId 제품 코드
 * @param userId 입고자(출고자) 사번. 재선택하지 않으면 제품 코드가 온다(D-048)
 * @param postalCode 우편번호
 * @param address1 도로명 주소
 * @param address2 상세주소
 * @param count 수량
 * @param type 0 입고 / 1 출고
 */
public record BoundUpdate(
        int id,
        String productId,
        String userId,
        String postalCode,
        String address1,
        String address2,
        int count,
        int type) {
}
