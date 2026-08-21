package com.erflow.process;

import java.util.Map;

/**
 * 공정 검색 조건.
 *
 * <p>레거시가 칸에 따라 조건을 달리 만든다 — 공정ID·공정명은 {@code like} 이고
 * 우선순위만 <b>정확히 같은 값</b>을 찾는다.
 *
 * <pre>
 * id       -&gt; id like '%값%'
 * name     -&gt; name like '%값%'
 * priority -&gt; priority = 값
 * </pre>
 *
 * @param keyfield 검색할 칸
 * @param keyword 검색어
 */
public record ProcessSearch(String keyfield, String keyword) {

    private static final Map<String, String> LIKE_FIELDS = Map.of("id", "id", "name", "name");

    private static final String PRIORITY = "priority";

    /**
     * 조건 없는 전체 조회.
     *
     * @return 아무것도 걸지 않는 조건
     */
    public static ProcessSearch none() {
        return new ProcessSearch("", "");
    }

    /**
     * @return {@code like} 조건을 붙여야 하면 {@code true}
     */
    public boolean byText() {
        return LIKE_FIELDS.containsKey(keyfield);
    }

    /**
     * @return 우선순위 조건을 붙여야 하면 {@code true}
     */
    public boolean byPriority() {
        return PRIORITY.equals(keyfield) && priority() != null;
    }

    /**
     * @return 조건을 걸 컬럼명. 텍스트 조건이 아니면 {@code null}
     */
    public String column() {
        return LIKE_FIELDS.get(keyfield);
    }

    /**
     * @return 검색어 like 패턴
     */
    public String keywordPattern() {
        return "%" + (keyword == null ? "" : keyword) + "%";
    }

    /**
     * 우선순위 검색어를 숫자로 읽는다.
     *
     * <p>레거시는 {@code Integer.parseInt} 를 그대로 불러 숫자가 아니면 그 자리에서
     * 죽는다. 그 죽음은 옮기지 않고 «조건 없음» 으로 본다.
     *
     * @return 숫자. 읽을 수 없으면 {@code null}
     */
    public Integer priority() {
        try {
            return Integer.valueOf(keyword.trim());
        } catch (NumberFormatException | NullPointerException e) {
            return null;
        }
    }
}
