package com.erflow.common;

import java.util.List;
import java.util.stream.IntStream;

/**
 * 근무 상태 — 코드 하나에 이름 하나, 색 하나.
 *
 * <h2>한 벌로 합쳤다(D-125)</h2>
 *
 * <p>레거시는 같은 코드를 네 곳에서 따로 읽었다. 관리자 표({@code admin.jsp}), 원
 * 그래프({@code adminGraph.js}), 프로필 달력, 근태 확인 표가 저마다 배열과 색을 박아
 * 두었고, 1단계는 그대로 옮기며 안건으로 남겼다(D-068). 어긋난 자리가 둘 있었다.
 *
 * <ul>
 *   <li><b>그래프의 3·4 가 뒤바뀌어</b> 조퇴와 지각이 서로의 이름을 달고 나왔다.
 *       나머지 세 곳이 «3 조퇴 · 4 지각» 으로 일치하므로 그래프가 틀린 것이다.</li>
 *   <li><b>조퇴 색이 화면마다 달랐다</b> — 달력은 {@code skyblue}, 근태표는 보라
 *       ({@code #8507F7}). 반차와 연차는 반대로 <b>둘 다 {@code #dbdbdb}</b> 라
 *       구분이 안 됐고, 범례에는 반차가 아예 없었다.</li>
 * </ul>
 *
 * <p>이제 이 클래스가 유일한 출처다. 이름은 여기서 나오고, 색은 여기서 정한 <b>클래스
 * 이름</b>을 타고 {@code app.css} 의 {@code --color-status-*} 로 간다. Java 는 «무슨
 * 상태인가» 만 말하고 색은 디자인 체계가 정한다.
 *
 * <p>코드 1 은 «근무 중» 이다(2026-08-26, 사용자 판단). 코드 2 가 «퇴근» 이므로 1 은
 * «아직 일하는 중» 이라는 뜻이고, 근무 시간을 계속 세는 동작과도 맞는다.
 *
 * <pre>
 * 코드   0     1        2     3     4     5     6
 *        결근  근무 중   퇴근  조퇴  지각  반차  연차
 * </pre>
 */
public final class WorkStatus {

    /** 이름. 코드 순서 그대로다. */
    private static final String[] LABELS = {
        "결근", "근무 중", "퇴근", "조퇴", "지각", "반차", "연차",
    };

    /**
     * 색을 고르는 클래스 이름. 코드 순서 그대로다.
     *
     * <p>색값은 여기 없다 — {@code app.css} 가 {@code .work-cell.is-absent} 처럼
     * 자리마다 다르게 쓴다. 달력은 바탕을 칠하고 근태표는 글자를 칠하지만 상태
     * 이름은 하나다.
     */
    private static final String[] CLASSES = {
        "is-absent", "is-working", "is-done", "is-early", "is-late", "is-half", "is-off",
    };

    private WorkStatus() {
    }

    /**
     * 상태 이름.
     *
     * @param code 상태 코드
     * @return 이름. 아는 코드가 아니면 빈 글자(레거시는 배열 밖을 읽어 죽는다)
     */
    public static String label(int code) {
        return known(code) ? LABELS[code] : "";
    }

    /**
     * 색을 고르는 클래스 이름.
     *
     * @param code 상태 코드
     * @return 클래스 이름. 아는 코드가 아니면 {@code null} — 칠하지 않는다
     */
    public static String styleClass(int code) {
        return known(code) ? CLASSES[code] : null;
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

    /**
     * 범례에 쓰는 전체 목록.
     *
     * <p>범례를 손으로 적어 두면 색을 고칠 때 같이 안 고쳐진다. 실제로 레거시 범례는
     * 다섯 칸뿐이라 반차가 빠져 있었고, 연차와 같은 색이라 알아채기도 어려웠다.
     *
     * @return 코드 순서대로 늘어놓은 상태
     */
    public static List<Entry> all() {
        return IntStream.range(0, LABELS.length)
                .mapToObj(code -> new Entry(code, LABELS[code], CLASSES[code]))
                .toList();
    }

    private static boolean known(int code) {
        return code >= 0 && code < LABELS.length;
    }

    /**
     * 상태 하나.
     *
     * @param code       상태 코드
     * @param label      이름
     * @param styleClass 색을 고르는 클래스 이름
     */
    public record Entry(int code, String label, String styleClass) {
    }
}
