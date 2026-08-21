package com.erflow.task;

import java.util.Map;

/**
 * 수·발주 상태 코드와 라벨.
 *
 * <p>출처: {@code repository/TaskRepository} 의 {@code taskStatus} 맵.
 * 코드 1·2·3 에 각각 «진행»·«완료»·«미확인». 레거시가 이 라벨을 목록의 상태 칸에
 * 찍는다.
 *
 * <p>주의: 등록·수정 화면의 select 는 같은 코드를 «진행중»·«완료»·«미확인» 으로 적는다.
 * «진행» 과 «진행중» 이 갈리는데 레거시가 자리마다 다르게 써 둔 것이라 그대로 둔다 —
 * 이 클래스는 <b>목록 표시용</b> 라벨만 담는다.
 */
public final class TaskStatus {

    private static final Map<Integer, String> LABELS =
            Map.of(1, "진행", 2, "완료", 3, "미확인");

    private TaskStatus() {
    }

    /**
     * 상태 코드의 목록 표시 라벨.
     *
     * @param code 상태 코드
     * @return 라벨. 코드가 1·2·3 이 아니면 {@code null}(레거시 {@code map.get} 과 같다)
     */
    public static String label(int code) {
        return LABELS.get(code);
    }
}
