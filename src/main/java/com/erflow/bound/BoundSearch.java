package com.erflow.bound;

import java.util.Map;

/**
 * 입·출고 목록 검색 조건.
 *
 * <p>레거시 {@code getInbounds}/{@code getOutbounds} 를 옮겼다. 제품ID·제품명·입고자는
 * {@code like}, 날짜는 {@code bounded_at >= 값} 이다. 화이트리스트 밖의 keyfield 는
 * 조건을 만들지 않는다.
 *
 * @param keyfield 검색 대상(id/productName/userName/date)
 * @param keyword 검색어
 */
public record BoundSearch(String keyfield, String keyword) {

    /** 화면 검색 항목 -> {@code bound_view} 컬럼. like 로 거는 것. */
    private static final Map<String, String> LIKE_FIELDS = Map.of(
            "id", "id",
            "productName", "product_name",
            "userName", "user_name");

    /**
     * 조건 없는 전체 조회.
     *
     * @return 아무 조건도 없는 검색
     */
    public static BoundSearch none() {
        return new BoundSearch("", "");
    }

    /**
     * @return like 로 거는 검색이면 {@code true}
     */
    public boolean isLike() {
        return keyfield != null && LIKE_FIELDS.containsKey(keyfield);
    }

    /**
     * @return 날짜(이상) 검색이면 {@code true}
     */
    public boolean isDate() {
        return "date".equals(keyfield);
    }

    /**
     * @return 조건을 걸어야 하면 {@code true}
     */
    public boolean isActive() {
        return isLike() || isDate();
    }

    /**
     * @return like 검색 대상 컬럼명. like 검색이 아니면 {@code null}
     */
    public String column() {
        return isLike() ? LIKE_FIELDS.get(keyfield) : null;
    }

    /**
     * @return 앞뒤에 {@code %} 를 붙인 검색어
     */
    public String pattern() {
        return "%" + (keyword == null ? "" : keyword) + "%";
    }

    /**
     * @return 날짜 비교 값. 레거시가 {@code bounded_at >= ?} 에 다듬지 않고 넣는다
     */
    public String dateValue() {
        return keyword == null ? "" : keyword;
    }
}
