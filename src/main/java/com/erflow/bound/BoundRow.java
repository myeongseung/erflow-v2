package com.erflow.bound;

/**
 * 입·출고 목록 한 줄. {@code bound_view} 한 행이다.
 *
 * <p>레거시 뷰에는 {@code user_id} 컬럼이 없어, 목록은 이름·주소·수량만 보여준다.
 *
 * @param id 번호
 * @param productName 제품명
 * @param userName 입고자(출고자) 이름
 * @param address1 도로명 주소
 * @param address2 상세주소
 * @param boundedAt 입고(출고) 시간
 * @param count 수량
 */
public record BoundRow(
        int id,
        String productName,
        String userName,
        String address1,
        String address2,
        String boundedAt,
        int count) {

    /**
     * 목록에 보이는 주소.
     *
     * <p>레거시는 {@code address1 + " " + address2} 로 한 칸에 넣었다.
     *
     * @return 도로명과 상세주소를 이은 문자열
     */
    public String fullAddress() {
        return (address1 == null ? "" : address1) + " " + (address2 == null ? "" : address2);
    }
}
