package com.erflow.profile;

/**
 * 근무 기록 한 건. {@code work_view} 한 행에서 달력이 쓰는 칸만.
 *
 * @param startedAt 출근 시각. {@code yyyy-MM-dd HH:mm:ss} 글자
 * @param endedAt 퇴근 시각
 * @param status 근무 상태 코드
 */
public record ProfileWork(String startedAt, String endedAt, int status) {

    /**
     * 달력이 실제로 쓰는 상태 코드.
     *
     * <p>출근도 퇴근도 찍히지 않았으면 무슨 코드가 들어 있든 결근으로 본다.
     *
     * @return 상태 코드
     */
    public int effectiveStatus() {
        return startedAt == null && endedAt == null ? 0 : status;
    }
}
