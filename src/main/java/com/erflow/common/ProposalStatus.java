package com.erflow.common;

import java.util.Map;

/**
 * 결재 상태 코드와 라벨·색.
 *
 * <p>출처: {@code repository/ProposalRepository} 의 {@code proposalStatus} 맵과
 * {@code admin.jsp} 의 {@code switch (result)}.
 *
 * <pre>
 * 0  결재 대기 중   #E4E4E4 (기본색)
 * 1  승인          #C2FF63
 * 2  반려          #EA7B86
 * 3  결재 진행 중   #FFFF40
 * </pre>
 *
 * <p>대시보드와 메인 화면이 함께 쓴다. 코드표를 두 벌 두면 한쪽만 고쳐진다.
 *
 * <p>상태 0 은 색이 따로 없어 기본색으로 남는다. 결재 화면에서 «내 차례가 끝났다» 를
 * 뜻하는 값인데(D-051) 여기서는 «결재 대기 중» 으로 읽힌다 — 레거시 그대로다.
 */
public final class ProposalStatus {

    private static final Map<Integer, String> LABELS = Map.of(
            0, "결재 대기 중", 1, "승인", 2, "반려", 3, "결재 진행 중");

    private static final Map<Integer, String> COLORS = Map.of(
            1, "#C2FF63", 2, "#EA7B86", 3, "#FFFF40");

    private static final String DEFAULT_COLOR = "#E4E4E4";

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
     * 상태 칸의 배경색.
     *
     * @param code 상태 코드
     * @return 색. 1·2·3 이 아니면 기본색
     */
    public static String color(int code) {
        return COLORS.getOrDefault(code, DEFAULT_COLOR);
    }
}
