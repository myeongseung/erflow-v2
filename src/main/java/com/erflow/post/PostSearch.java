package com.erflow.post;

import java.util.Map;

/**
 * 게시글 목록 검색 조건.
 *
 * <p>레거시 {@code PostServiceImpl} 의 {@code switch (keyfield)} 를 옮겼다.
 * 화이트리스트 밖 값은 조건을 만들지 않으므로 주입할 자리가 없다.
 *
 * <h2>레거시가 목록과 개수를 다른 조건으로 분기한다</h2>
 *
 * <pre>
 * getPostViews : if (keyfield != null &amp;&amp; !keyfield.trim().equals("")) // keyfield 기준
 * getTotalCount: if (keyword  != null &amp;&amp; !keyword.trim().equals(""))  // keyword 기준
 * </pre>
 *
 * <p>검색 항목만 고르고 검색어를 비운 채 조회하면, 목록은 {@code subject like '%%'}
 * 로 걸러지고 개수는 전체를 센다. {@code subject} 가 {@code NULL} 인 글이 있으면
 * 목록에서만 빠져 개수와 어긋난다. 실데이터에는 {@code NULL} 제목이 없어 드러나지
 * 않는다. 결함이지만 이관에서 고치지 않는다 — {@code migration/design/00-decisions.md}
 * 의 D-024 참조.
 *
 * @param keyfield 검색 대상
 * @param keyword 검색어
 */
public record PostSearch(String keyfield, String keyword) {

    /** 화면의 검색 항목과 {@code post_view} 컬럼의 대응. 레거시 switch 그대로다. */
    private static final Map<String, String> FIELDS = Map.of(
            "subject", "subject",
            "author", "name");

    /**
     * 조건 없는 전체 조회.
     *
     * @return 검색어가 없는 조건
     */
    public static PostSearch none() {
        return new PostSearch(null, null);
    }

    /**
     * 목록 질의에 조건을 붙일지 여부.
     *
     * @return 레거시 {@code getPostViews} 의 분기 조건
     */
    public boolean activeForList() {
        return keyfield != null && !keyfield.isBlank() && FIELDS.containsKey(keyfield);
    }

    /**
     * 개수 질의에 조건을 붙일지 여부.
     *
     * <p>레거시 {@code getTotalCount} 는 {@code keyword} 를 본다. 목록과 다르다.
     *
     * @return 레거시 {@code getTotalCount} 의 분기 조건
     */
    public boolean activeForCount() {
        return keyword != null && !keyword.isBlank() && FIELDS.containsKey(keyfield);
    }

    /**
     * 검색 대상 컬럼명.
     *
     * @return 화이트리스트에서 찾은 컬럼명. 조건이 없으면 {@code null}
     */
    public String column() {
        return keyfield == null ? null : FIELDS.get(keyfield);
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
