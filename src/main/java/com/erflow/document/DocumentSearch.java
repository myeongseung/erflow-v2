package com.erflow.document;

/**
 * 문서 검색 조건.
 *
 * <p>칸마다 조건이 다르다 — 제목·양식명은 {@code like} 이고 문서ID 만 <b>정확히 같은
 * 값</b>을 찾는다.
 *
 * <pre>
 * subject  -&gt; subject like '%값%'
 * template -&gt; template_name like '%값%'
 * id       -&gt; id = 값
 * </pre>
 *
 * @param keyfield 검색할 칸
 * @param keyword 검색어
 */
public record DocumentSearch(String keyfield, String keyword) {

    /**
     * 조건 없는 전체 조회.
     *
     * @return 아무것도 걸지 않는 조건
     */
    public static DocumentSearch none() {
        return new DocumentSearch("", "");
    }

    /**
     * @return 제목으로 찾으면 {@code true}
     */
    public boolean bySubject() {
        return "subject".equals(keyfield);
    }

    /**
     * @return 양식명으로 찾으면 {@code true}
     */
    public boolean byTemplate() {
        return "template".equals(keyfield);
    }

    /**
     * @return 문서번호로 찾으면 {@code true}
     */
    public boolean byId() {
        return "id".equals(keyfield) && id() != null;
    }

    /**
     * @return 검색어 like 패턴
     */
    public String keywordPattern() {
        return "%" + (keyword == null ? "" : keyword) + "%";
    }

    /**
     * 검색어를 문서번호로 읽는다.
     *
     * <p>레거시는 {@code Long.parseLong} 을 그대로 불러 숫자가 아니면 그 자리에서
     * 죽는다. 그 죽음은 옮기지 않고 «조건 없음» 으로 본다.
     *
     * @return 문서번호. 읽을 수 없으면 {@code null}
     */
    public Long id() {
        try {
            return Long.valueOf(keyword.trim());
        } catch (NumberFormatException | NullPointerException e) {
            return null;
        }
    }
}
