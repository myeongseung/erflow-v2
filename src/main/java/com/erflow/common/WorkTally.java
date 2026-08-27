package com.erflow.common;

/**
 * 근무 통계 한 벌.
 *
 * <h2>한 달 합계가 화면 어디에도 없었다(D-077)</h2>
 *
 * <p>프로필 달력도 근태 확인 표도 한 달을 두 줄로 그린다 — 1~16일, 17일~말일.
 * 오른쪽 끝의 «정상·지각·조퇴·연차» 는 <b>그 줄만의 합계</b>다. 레거시가 두 줄을
 * 그리는 사이에 카운터를 0으로 되돌리기 때문이다.
 *
 * <p>두 줄에 같은 이름의 칸이 위아래로 붙어 있어 <b>합계처럼 보이지만 아니다.</b>
 * 한 달 합계를 알려면 눈으로 더해야 했다. 1단계는 «화면에 없던 숫자를 만드는 일»
 * 이라 그대로 옮겼고(D-077), 이제 그 숫자를 만든다.
 *
 * @param normal   정상 근무 일수
 * @param late     지각 일수
 * @param leave    조퇴 일수
 * @param vacation 연차 일수. 반차가 0.5로 더해져 소수가 된다
 */
public record WorkTally(int normal, int late, int leave, double vacation) {

    /**
     * 두 줄을 더한다.
     *
     * @param first  윗줄
     * @param second 아랫줄
     * @return 한 달 합계
     */
    public static WorkTally sum(WorkTally first, WorkTally second) {
        return new WorkTally(
                first.normal + second.normal,
                first.late + second.late,
                first.leave + second.leave,
                first.vacation + second.vacation);
    }

    /**
     * 화면에 찍히는 연차.
     *
     * <p>레거시가 {@code double} 을 그대로 찍어 «0.0», «1.5» 처럼 보인다. 반차가
     * 0.5로 세어지므로 소수 자리가 뜻을 갖는다 — 그대로 둔다.
     *
     * @return 소수점이 붙은 숫자 글자
     */
    public String vacationLabel() {
        return String.valueOf(vacation);
    }
}
