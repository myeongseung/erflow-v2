package com.erflow.profile;

import com.erflow.common.WorkTally;
import java.util.List;

/**
 * 근무 현황 표의 한 줄. 칸 16개와 그 줄만의 통계.
 *
 * @param days 칸 16개
 * @param normal 정상 근무 일수
 * @param late 지각 일수
 * @param leave 조퇴 일수
 * @param vacation 연차 일수. 반차가 0.5로 더해져 소수가 된다
 */
public record WorkHalf(List<WorkDay> days, int normal, int late, int leave, double vacation) {

    /**
     * @return 이 줄만의 통계
     */
    public WorkTally tally() {
        return new WorkTally(normal, late, leave, vacation);
    }

    /**
     * 화면에 찍히는 연차.
     *
     * <p>레거시가 {@code double} 을 그대로 찍어 «0.0», «1.5» 처럼 보인다.
     *
     * @return 소수점이 붙은 숫자 글자
     */
    public String vacationLabel() {
        return String.valueOf(vacation);
    }
}
