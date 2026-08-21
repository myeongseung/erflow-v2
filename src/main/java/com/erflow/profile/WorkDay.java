package com.erflow.profile;

/**
 * 근무 현황 표의 칸 하나. 머리글(날짜)과 몸통(색) 두 자리에 같은 값이 쓰인다.
 *
 * @param day 날짜
 * @param inMonth 그 달에 있는 날인지. 아랫줄 끝의 남는 칸은 {@code false} 다
 * @param headerStyle 머리글 칸의 {@code style}. 평일은 {@code null}
 * @param cellStyle 몸통 칸의 {@code style}. 달 밖의 칸은 {@code null}
 */
public record WorkDay(int day, boolean inMonth, String headerStyle, String cellStyle) {

    /** 일요일 날짜 색. */
    private static final String SUNDAY_STYLE = "color: red;";

    /** 토요일 날짜 색. */
    private static final String SATURDAY_STYLE = "color: blue;";

    /** 몸통 칸의 기본 높이. 색이 없어도 이 값은 붙는다. */
    private static final String HEIGHT = "height: 50px;";

    /**
     * 그 달에 있는 날.
     *
     * @param day 날짜
     * @param sunday 일요일인지
     * @param saturday 토요일인지
     * @param color 배경색. 칠하지 않으면 {@code null}
     * @return 칸
     */
    public static WorkDay inside(int day, boolean sunday, boolean saturday, String color) {
        String header = null;
        if (sunday) {
            header = SUNDAY_STYLE;
        } else if (saturday) {
            header = SATURDAY_STYLE;
        }
        String cell = color == null ? HEIGHT : HEIGHT + " background-color: " + color + ";";
        return new WorkDay(day, true, header, cell);
    }

    /**
     * 아랫줄 끝의 남는 칸. 30일 달이면 31·32 자리가 여기 해당한다.
     *
     * <p>머리글도 몸통도 <b>속성 없이 비어 있다</b> — 몸통에는 높이조차 붙지 않아
     * 줄 끝의 칸만 납작해진다. 레거시 그대로다.
     *
     * @param day 날짜
     * @return 빈 칸
     */
    public static WorkDay outside(int day) {
        return new WorkDay(day, false, null, null);
    }

    /**
     * @return 머리글에 찍히는 두 자리 날짜. 달 밖의 칸은 빈 글자
     */
    public String label() {
        return inMonth ? String.format("%02d", day) : "";
    }
}
