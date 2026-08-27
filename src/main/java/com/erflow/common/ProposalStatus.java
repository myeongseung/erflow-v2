package com.erflow.common;

import java.util.Map;

/**
 * 결재 상태 코드와 라벨·색.
 *
 * <p>출처: {@code repository/ProposalRepository} 의 {@code proposalStatus} 맵과
 * {@code admin.jsp} 의 {@code switch (result)}.
 *
 * <pre>
 * 0  결재 대기 중   is-pending
 * 1  승인          is-approved
 * 2  반려          is-rejected
 * 3  결재 진행 중   is-running
 * </pre>
 *
 * <p>색값은 여기 없다. 레거시는 {@code #C2FF63} 같은 형광색을 Java 에서
 * 인라인 style 로 뿜었고, 그래서 화면을 다시 그려도 색만 남아 떠다니었다.
 * 이제 클래스 이름만 내보내고 {@code app.css} 가 정한다 — {@code WorkStatus} 와
 * 같은 방식이다(D-125).
 *
 * <p>대시보드와 메인 화면이 함께 쓴다. 코드표를 두 벌 두면 한쪽만 고쳐진다.
 *
 * <p>상태 0 은 색이 따로 없어 기본색으로 남는다. 결재 화면에서 «내 차례가 끝났다» 를
 * 뜻하는 값인데(D-051) 여기서는 «결재 대기 중» 으로 읽힌다 — 레거시 그대로다.
 */
public final class ProposalStatus {

    private static final Map<Integer, String> LABELS = Map.of(
            0, "결재 대기 중", 1, "승인", 2, "반려", 3, "결재 진행 중");

    private static final Map<Integer, String> CLASSES = Map.of(
            1, "is-approved", 2, "is-rejected", 3, "is-running");

    private static final String DEFAULT_CLASS = "is-pending";

    private ProposalStatus() {
    }

    /**
     * 상태 라벨.
     *
     * @param code 상태 코드
     * @return 라벨. 아는 코드가 아니면 {@code null}(레거시 {@code map.get} 과 같다)
     */
    public static String label(int code) {
        return LABELS.get(code);
    }

    /**
     * 상태 알약의 클래스 이름.
     *
     * @param code 상태 코드
     * @return 클래스 이름. 1·2·3 이 아니면 기본
     */
    public static String styleClass(int code) {
        return CLASSES.getOrDefault(code, DEFAULT_CLASS);
    }
}
