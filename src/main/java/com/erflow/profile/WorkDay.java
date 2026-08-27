package com.erflow.profile;

/**
 * 근무 현황 표의 칸 하나. 머리글(날짜)과 몸통(색) 두 자리에 같은 값이 쓰인다.
 *
 * <p>레거시는 여기서 {@code style="height: 50px; background-color: greenyellow;"} 를
 * 만들어 뱉었다. 색값이 Java 안에 있으니 화면마다 달라졌고 실제로 달랐다 — 같은
 * 조퇴가 달력에선 {@code skyblue}, 근태표에선 보라였다.
 *
 * <p>이제 <b>클래스 이름만</b> 내보낸다(D-125). 높이도 색도 {@code app.css} 의
 * {@code .work-cell} 이 정한다. Java 는 «무슨 상태인가» 만 말한다.
 *
 * @param day 날짜
 * @param inMonth 그 달에 있는 날인지. 아랫줄 끝의 남는 칸은 {@code false} 다
 * @param headerClass 머리글 칸의 class. 평일은 {@code null}
 * @param cellClass 몸통 칸의 class. 달 밖의 칸은 {@code null}
 */
public record WorkDay(int day, boolean inMonth, String headerClass, String cellClass) {

    /** 일요일 날짜 색. */
    private static final String SUNDAY_CLASS = "is-sunday";

    /** 토요일 날짜 색. */
    private static final String SATURDAY_CLASS = "is-saturday";

    /** 몸통 칸의 바탕. 상태가 없어도 이 이름은 붙는다 — 높이가 여기 걸려 있다. */
    private static final String CELL = "work-cell";

    /**
     * 그 달에 있는 날.
     *
     * @param day 날짜
     * @param sunday 일요일인지
     * @param saturday 토요일인지
     * @param statusClass 상태 클래스({@code WorkStatus.styleClass}). 칠하지 않으면 {@code null}
     * @return 칸
     */
    public static WorkDay inside(int day, boolean sunday, boolean saturday, String statusClass) {
        String header = null;
        if (sunday) {
            header = SUNDAY_CLASS;
        } else if (saturday) {
            header = SATURDAY_CLASS;
        }
        String cell = statusClass == null ? CELL : CELL + " " + statusClass;
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
