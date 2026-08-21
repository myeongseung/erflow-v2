package com.erflow.admin.home;

/**
 * 근무 상태 코드와 라벨.
 *
 * <p><b>같은 코드를 두 곳이 다르게 읽는다.</b> 출퇴근 표는 코드 1 을 «근무 중» 으로,
 * 근무 현황 그래프는 «출근» 으로 적는다. 레거시가 두 자리에 배열을 따로 박아 둔
 * 결과이며 그대로 옮긴다(D-068).
 *
 * <pre>
 * 코드   0     1        2     3     4     5     6
 * 표     결근  근무 중   퇴근  조퇴  지각  반차  연차
 * 그래프  결근  출근     퇴근  조퇴  지각  반차  연차
 * </pre>
 */
public final class WorkStatus {

    /** 출퇴근 표가 쓰는 라벨. 레거시 {@code admin.jsp} 의 배열이다. */
    private static final String[] TABLE = {
        "결근", "근무 중", "퇴근", "조퇴", "지각", "반차", "연차",
    };

    /** 근무 현황 그래프가 쓰는 라벨. 레거시 {@code adminGraph.js} 의 배열이다. */
    private static final String[] GRAPH = {
        "결근", "출근", "퇴근", "조퇴", "지각", "반차", "연차",
    };

    private WorkStatus() {
    }

    /**
     * 출퇴근 표의 라벨.
     *
     * @param code 상태 코드
     * @return 라벨. 아는 코드가 아니면 빈 문자열(레거시는 배열 밖을 읽어 죽는다)
     */
    public static String tableLabel(int code) {
        return code >= 0 && code < TABLE.length ? TABLE[code] : "";
    }

    /**
     * 그래프의 라벨.
     *
     * @param code 상태 코드
     * @return 라벨. 아는 코드가 아니면 빈 문자열
     */
    public static String graphLabel(int code) {
        return code >= 0 && code < GRAPH.length ? GRAPH[code] : "";
    }

    /**
     * 근무 시간을 세는 상태인지.
     *
     * <p>레거시는 코드 1·2 에만 시간을 찍는다.
     *
     * @param code 상태 코드
     * @return 시간을 찍어야 하면 {@code true}
     */
    public static boolean counted(int code) {
        return code == 1 || code == 2;
    }
}
