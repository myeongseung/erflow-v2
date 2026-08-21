package com.erflow.unit;

import java.util.Map;

/**
 * 목록 검색 조건.
 *
 * <p>레거시는 {@code keyfield} 를 컬럼명으로 바꿔 SQL 에 이어 붙였다. 화이트리스트에
 * 없는 값은 조건 자체를 만들지 않았고, 그 덕분에 주입이 막혀 있었다. 같은 방식을
 * 유지한다 — 목록만 옮기고 검사를 빼면 없던 구멍이 생긴다.
 *
 * @param keyfield 검색 대상. 화이트리스트 밖이면 전체 조회로 떨어진다
 * @param keyword 검색어
 */
public record UnitSearch(String keyfield, String keyword) {

    /**
     * 화면의 검색 항목과 {@code unit_view} 컬럼의 대응.
     *
     * <p>레거시 {@code UnitServiceImpl.getUnits} 의 keys/values 배열을 그대로 옮겼다.
     */
    private static final Map<String, String> FIELDS = Map.of(
            "id", "id",
            "name", "unit_name",
            "charger", "user_name",
            "document", "document_name",
            "status", "status",
            "date", "created_at");

    /**
     * 조건 없는 전체 조회.
     *
     * @return 검색어가 없는 조건
     */
    public static UnitSearch none() {
        return new UnitSearch(null, null);
    }

    /**
     * 검색 조건이 성립하는지 여부.
     *
     * <p>레거시는 {@code keyfield} 가 화이트리스트에 있을 때만 조건을 붙였다.
     * 검색어가 비어 있어도 조건을 붙였으므로(빈 문자열 like) 그 동작을 유지한다.
     *
     * @return 조건을 걸어야 하면 {@code true}
     */
    public boolean active() {
        return keyfield != null && !keyfield.isBlank() && FIELDS.containsKey(keyfield);
    }

    /**
     * 검색 대상 컬럼명.
     *
     * @return 화이트리스트에서 찾은 컬럼명. 조건이 없으면 {@code null}
     */
    public String column() {
        return active() ? FIELDS.get(keyfield) : null;
    }

    /**
     * like 패턴.
     *
     * @return 앞뒤에 {@code %} 를 붙인 검색어
     */
    public String pattern() {
        return "%" + (keyword == null ? "" : keyword) + "%";
    }
}
