package com.erflow.user;

/**
 * 사용자 찾기 조건. 부서·직급·이름 셋을 {@code and} 로 좁힌다.
 *
 * <p>레거시 {@code UserServiceImpl.getUserViews(keyfield1, keyfield2, keyword)} 가
 * 조건을 만드는 방식을 그대로 옮겼다. 셋 다 값이 있을 때만 붙고, 붙을 때는
 * {@code like '%값%'} 이다.
 *
 * <h2>부서·직급도 {@code like} 다</h2>
 *
 * <p>화면에서 콤보로 고른 값이라 전체 일치처럼 보이지만 조건은 부분 일치다.
 * 부서명이 다른 부서명을 품고 있으면(«영업» 과 «해외영업») 한쪽을 골라도 양쪽이
 * 나온다. 지금 데이터에서 그런 짝이 없어 드러나지 않을 뿐이다.
 *
 * <h2>검색어는 이름만 훑는다</h2>
 *
 * <p>표에 사번이 보이지만 조건은 {@code name like ?} 뿐이다. 사번을 넣으면 아무것도
 * 나오지 않는다.
 *
 * <h2>«비었다»의 기준은 {@code trim} 이다</h2>
 *
 * <p>레거시가 {@code value.trim().equals("")} 로 판정하면서 <b>패턴에는 다듬지 않은
 * 값</b>을 넣는다. 그래서 공백만 넣으면 조건이 아예 붙지 않고, {@code "김 "} 처럼
 * 뒤에 공백이 붙은 검색어는 그 공백까지 포함해 찾는다. 그대로 옮긴다.
 *
 * @param dept 부서명
 * @param job 직급명
 * @param keyword 이름 검색어
 */
public record UserSearch(String dept, String job, String keyword) {

    /**
     * 조건 없는 전체 조회. 팝업을 열자마자 이것이 돌아간다.
     *
     * @return 아무것도 걸지 않는 조건
     */
    public static UserSearch none() {
        return new UserSearch("", "", "");
    }

    /**
     * @return 부서 조건을 붙여야 하면 {@code true}
     */
    public boolean hasDept() {
        return filled(dept);
    }

    /**
     * @return 직급 조건을 붙여야 하면 {@code true}
     */
    public boolean hasJob() {
        return filled(job);
    }

    /**
     * @return 이름 조건을 붙여야 하면 {@code true}
     */
    public boolean hasKeyword() {
        return filled(keyword);
    }

    /**
     * @return 부서명 like 패턴
     */
    public String deptPattern() {
        return pattern(dept);
    }

    /**
     * @return 직급명 like 패턴
     */
    public String jobPattern() {
        return pattern(job);
    }

    /**
     * @return 이름 like 패턴
     */
    public String keywordPattern() {
        return pattern(keyword);
    }

    private static boolean filled(String value) {
        // isBlank() 가 아니라 trim() 이다. 레거시가 그렇게 판정한다 — 유니코드 공백
        // 몇 가지에서 둘의 답이 갈린다.
        return value != null && !value.trim().isEmpty();
    }

    private static String pattern(String value) {
        // 다듬지 않은 값을 넣는다. 레거시가 판정에만 trim 을 쓴다.
        return "%" + value + "%";
    }
}
