package com.erflow.work;

import com.erflow.common.WorkTally;
import java.util.List;

/**
 * 근태 표에서 한 사람의 반 줄. 칸 16개와 그 줄만의 통계.
 *
 * <p>통계가 줄마다 따로 세어지는 것은 프로필과 같다(D-077) — 레거시가 두 줄을
 * 그리는 사이에 카운터를 0으로 되돌린다.
 *
 * @param cells 칸 16개
 * @param normal 정상 근무 일수
 * @param late 지각 일수
 * @param leave 조퇴 일수
 * @param vacation 연차 일수. 반차가 0.5로 더해져 소수가 된다
 */
public record AttendanceHalf(
        List<AttendanceCell> cells, int normal, int late, int leave, double vacation) {

    /**
     * @return 이 줄만의 통계
     */
    public WorkTally tally() {
        return new WorkTally(normal, late, leave, vacation);
    }

    /**
     * 화면에 찍히는 연차. 레거시가 {@code double} 을 그대로 찍어 «0.0» 처럼 보인다.
     *
     * @return 소수점이 붙은 숫자 글자
     */
    public String vacationLabel() {
        return String.valueOf(vacation);
    }
}
