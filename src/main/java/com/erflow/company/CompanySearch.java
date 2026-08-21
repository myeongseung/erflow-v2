package com.erflow.company;

import java.util.List;

/**
 * 협력업체 목록 검색 조건.
 *
 * <p>레거시는 검색 방식이 둘이었다.
 *
 * <pre>
 * keyfield=name   and name like '%검색어%'
 * keyfield=field  업종명에 검색어가 들어간 업종코드를 모아  and field in (?, ?, ...)
 * 그 외            조건 없음
 * </pre>
 *
 * @param keyfield 검색 대상
 * @param keyword 검색어
 * @param fieldCodes {@code keyfield=field} 일 때 걸리는 업종코드. 그 외에는 빈 목록
 */
public record CompanySearch(String keyfield, String keyword, List<String> fieldCodes) {

    /** 협력업체명으로 검색. */
    public static final String BY_NAME = "name";

    /** 업체 종목으로 검색. */
    public static final String BY_FIELD = "field";

    /**
     * 조건 없는 전체 조회.
     *
     * @return 검색어가 없는 조건
     */
    public static CompanySearch none() {
        return new CompanySearch(null, null, List.of());
    }

    /**
     * 화면에서 넘어온 값으로 조건을 만든다.
     *
     * @param keyfield 검색 대상
     * @param keyword 검색어
     * @param fieldCodes 업종 검색일 때 미리 푼 업종코드
     * @return 검색 조건
     */
    public static CompanySearch of(String keyfield, String keyword, List<String> fieldCodes) {
        return new CompanySearch(keyfield, keyword, fieldCodes == null ? List.of() : fieldCodes);
    }

    /**
     * 이름으로 거르는지 여부.
     *
     * @return {@code keyfield} 가 {@code name} 이면 {@code true}
     */
    public boolean byName() {
        return BY_NAME.equals(keyfield);
    }

    /**
     * 업종으로 거르는지 여부.
     *
     * @return {@code keyfield} 가 {@code field} 이면 {@code true}
     */
    public boolean byField() {
        return BY_FIELD.equals(keyfield);
    }

    /**
     * 걸리는 업종이 하나도 없는 검색인지.
     *
     * <p>레거시는 이 경우 빈 {@code IN ()} 을 만들다 예외로 죽었다
     * ({@code "".substring(1)}). 결과가 없다는 뜻이므로 빈 목록을 돌려주는 것이 맞다.
     * 근거: {@code migration/design/00-decisions.md} D-017.
     *
     * @return 업종 검색인데 걸리는 코드가 없으면 {@code true}
     */
    public boolean matchesNothing() {
        return byField() && fieldCodes.isEmpty();
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
