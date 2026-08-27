package com.erflow.work;

/**
 * 근태 표의 칸 하나.
 *
 * <p>프로필 달력(D-077)과 달리 이 표는 <b>글자</b>로 알린다 — 정상 근무는 일한 시간
 * (HH:mm), 나머지는 상태 이름이 색 글자로 찍힌다.
 *
 * <p>색값은 갖지 않는다. 상태 클래스만 들고 있고 {@code app.css} 의 {@code .att-cell}
 * 이 글자색을 정한다(D-125).
 *
 * @param text 찍히는 글자. 기록이 없거나 주말에 지워진 상태면 빈 글자
 * @param statusClass 상태 클래스({@code WorkStatus.styleClass}). 색이 없으면 {@code null}
 */
public record AttendanceCell(String text, String statusClass) {

    /** 칸의 바탕. 상태가 없어도 이 이름은 붙는다. */
    private static final String BASE = "att-cell";

    /** 기록이 없는 날. */
    static final AttendanceCell EMPTY = new AttendanceCell("", null);

    /**
     * @return {@code class} 속성값
     */
    public String cellClass() {
        return statusClass == null ? BASE : BASE + " " + statusClass;
    }
}
