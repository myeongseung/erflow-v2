package com.erflow.task;

import java.util.Map;

/**
 * 수·발주 목록 검색 조건.
 *
 * <p>레거시 {@code TaskServiceImpl.getTasks} 의 분기를 그대로 옮겼다. 검색 대상이
 * 셋으로 갈린다.
 *
 * <ul>
 *   <li>직원명·부서명·회사명 — {@code like '%값%'}
 *   <li>날짜 — {@code task_at >= 값} (부분 일치가 아니라 이상)
 *   <li>상태 — {@code status = 값} (정수로 읽는다)
 * </ul>
 *
 * <p>화이트리스트 밖의 {@code keyfield}(예: «전체조회»의 빈 값)는 조건을 만들지 않는다.
 * 컬럼명을 SQL 에 이어 붙이지 않고 분기로 고정하므로 주입할 자리가 없다.
 *
 * @param keyfield 검색 대상. 화이트리스트 밖이면 조건 없음
 * @param keyword 검색어
 */
public record TaskSearch(String keyfield, String keyword) {

    /** 화면 검색 항목 -> {@code task_view} 컬럼. like 로 거는 것만. */
    private static final Map<String, String> LIKE_FIELDS = Map.of(
            "userName", "user_name",
            "deptName", "dept_name",
            "companyName", "company_name");

    /**
     * 조건 없는 전체 조회.
     *
     * @return 아무 조건도 없는 검색
     */
    public static TaskSearch none() {
        return new TaskSearch("", "");
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
     * @return 상태(일치) 검색이면 {@code true}
     */
    public boolean isStatus() {
        return "status".equals(keyfield);
    }

    /**
     * @return 조건을 걸어야 하면 {@code true}
     */
    public boolean isActive() {
        return isLike() || isDate() || isStatus();
    }

    /**
     * like 검색 대상 컬럼명.
     *
     * @return 화이트리스트에서 찾은 컬럼명. like 검색이 아니면 {@code null}
     */
    public String column() {
        return isLike() ? LIKE_FIELDS.get(keyfield) : null;
    }

    /**
     * like 패턴.
     *
     * @return 앞뒤에 {@code %} 를 붙인 검색어
     */
    public String pattern() {
        return "%" + (keyword == null ? "" : keyword) + "%";
    }

    /**
     * 날짜 비교 값.
     *
     * @return 검색어 그대로. 레거시가 {@code task_at >= ?} 에 다듬지 않고 넣는다
     */
    public String dateValue() {
        return keyword == null ? "" : keyword;
    }

    /**
     * 상태 코드.
     *
     * @return 검색어를 정수로 읽은 값
     * @throws NumberFormatException 검색어가 정수가 아닐 때. 레거시도 이때 목록이 빈다
     */
    public int statusValue() {
        return Integer.parseInt(keyword);
    }

    /**
     * 상태 검색어가 정수로 읽히는지 미리 확인한다.
     *
     * <p>레거시는 {@code Integer.parseInt} 가 던지면 {@code catch} 로 삼켜 빈 목록을
     * 돌려줬다. 매퍼까지 가기 전에 걸러 같은 결과(빈 목록)를 낸다.
     *
     * @return 상태 검색인데 검색어가 정수가 아니면 {@code true}
     */
    public boolean isBrokenStatus() {
        if (!isStatus()) {
            return false;
        }
        try {
            Integer.parseInt(keyword);
            return false;
        } catch (NumberFormatException notANumber) {
            return true;
        }
    }
}
