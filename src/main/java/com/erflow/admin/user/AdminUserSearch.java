package com.erflow.admin.user;

import java.util.Set;

/**
 * 사원 검색 조건. 고른 칸 하나를 {@code like} 로 훑는다.
 *
 * <h2>레거시는 칸 이름을 SQL 에 그대로 이어 붙인다</h2>
 *
 * <pre>
 * additional = keyfield + " like ? and ";
 * String sql = "select * from user_view where " + additional + "id &lt;&gt; 'admin' ...";
 * </pre>
 *
 * <p>{@code keyfield} 는 요청 파라미터다. 검색어는 {@code ?} 로 묶이지만 <b>칸 이름은
 * 묶이지 않는다</b> — 주소창에 무엇을 적든 그대로 조건이 된다. 그래서 화면이 주는 다섯
 * 가지만 받는다(D-054).
 *
 * @param keyfield 검색할 칸. 화면 콤보가 주는 값
 * @param keyword 검색어
 */
public record AdminUserSearch(String keyfield, String keyword) {

    /**
     * 화면 콤보가 주는 칸. 이 밖의 값은 조건이 되지 못한다.
     *
     * <p>{@code user_view} 의 컬럼명이자 레거시 {@code userList.jsp} 의 {@code option}
     * 값이다. 둘이 같아야 하므로 한쪽만 고칠 수 없다.
     */
    private static final Set<String> FIELDS = Set.of("id", "name", "dept_name", "job_name");

    /**
     * 조건 없는 전체 조회.
     *
     * @return 아무것도 걸지 않는 조건
     */
    public static AdminUserSearch none() {
        return new AdminUserSearch("", "");
    }

    /**
     * 이 조건을 SQL 에 붙일 수 있는지 본다.
     *
     * <p>레거시는 «비어 있지 않고 {@code all} 도 아니면» 붙였다. {@code all} 은 화면
     * 어디에서도 오지 않는 값이며 «전체 조회» 는 빈 문자열이다.
     *
     * @return 검색 조건을 붙여야 하면 {@code true}
     */
    public boolean active() {
        return keyfield != null && FIELDS.contains(keyfield.trim());
    }

    /**
     * 조건을 걸 컬럼명.
     *
     * <p>화이트리스트를 통과한 값만 돌려주므로 SQL 에 그대로 놓아도 된다.
     *
     * @return 컬럼명. 조건이 없으면 {@code null}
     */
    public String column() {
        return active() ? keyfield.trim() : null;
    }

    /**
     * @return 검색어 like 패턴. 레거시는 다듬지 않은 값을 넣는다
     */
    public String keywordPattern() {
        return "%" + (keyword == null ? "" : keyword) + "%";
    }
}
