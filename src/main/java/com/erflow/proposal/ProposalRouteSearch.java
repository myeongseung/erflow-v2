package com.erflow.proposal;

/**
 * 결재라인 목록 검색 조건.
 *
 * <p>레거시 {@code getProposalRoutes} 를 옮겼다. 문서번호(id)는 <b>정확히 일치</b>,
 * 결재경로(route)는 {@code like} 다. 화이트리스트 밖의 keyfield 는 조건을 만들지 않는다.
 *
 * @param keyfield 검색 대상(id/route)
 * @param keyword 검색어
 */
public record ProposalRouteSearch(String keyfield, String keyword) {

    /**
     * 조건 없는 전체 조회.
     *
     * @return 아무 조건도 없는 검색
     */
    public static ProposalRouteSearch none() {
        return new ProposalRouteSearch("", "");
    }

    /**
     * @return 문서번호(정확 일치) 검색이면 {@code true}
     */
    public boolean isId() {
        return "id".equals(keyfield);
    }

    /**
     * @return 결재경로(부분 일치) 검색이면 {@code true}
     */
    public boolean isRoute() {
        return "route".equals(keyfield);
    }

    /**
     * @return 조건을 걸어야 하면 {@code true}
     */
    public boolean isActive() {
        return isId() || isRoute();
    }

    /**
     * @return 문서번호 비교 값. 레거시가 다듬지 않고 넣는다
     */
    public String idValue() {
        return keyword == null ? "" : keyword;
    }

    /**
     * @return 결재경로 like 패턴
     */
    public String routePattern() {
        return "%" + (keyword == null ? "" : keyword) + "%";
    }
}
