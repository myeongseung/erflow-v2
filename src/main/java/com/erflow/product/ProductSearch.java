package com.erflow.product;

import java.util.Map;

/**
 * 제품 검색 조건. 화면이 주는 두 칸 중 하나를 {@code like} 로 훑는다.
 *
 * <p>레거시는 칸 이름을 맵으로 바꿔 SQL 에 이어 붙인다.
 *
 * <pre>
 * productId   -&gt; id
 * productName -&gt; name
 * </pre>
 *
 * <p>맵에 없는 값이면 조건이 붙지 않는다 — 사원 리스트(D-054)와 달리 레거시가 여기서는
 * 이미 화이트리스트를 쓴다. 그 구조를 그대로 옮긴다.
 *
 * @param keyfield 검색할 칸. 화면 콤보가 주는 값
 * @param keyword 검색어
 */
public record ProductSearch(String keyfield, String keyword) {

    private static final Map<String, String> FIELDS =
            Map.of("productId", "id", "productName", "name");

    /**
     * 조건 없는 전체 조회.
     *
     * @return 아무것도 걸지 않는 조건
     */
    public static ProductSearch none() {
        return new ProductSearch("", "");
    }

    /**
     * @return 검색 조건을 붙여야 하면 {@code true}
     */
    public boolean active() {
        return FIELDS.containsKey(keyfield);
    }

    /**
     * @return 조건을 걸 컬럼명. 조건이 없으면 {@code null}
     */
    public String column() {
        return FIELDS.get(keyfield);
    }

    /**
     * @return 검색어 like 패턴. 레거시는 다듬지 않은 값을 넣는다
     */
    public String keywordPattern() {
        return "%" + (keyword == null ? "" : keyword) + "%";
    }
}
