package com.erflow.proposal;

/**
 * 한 문서에서 가장 멀리 간 결재 차례. «마지막 차례인가»를 판정하는 데만 쓴다.
 *
 * <p>레거시 {@code isFinalStep} 은 이 한 행을 읽어 {@code step} 이 결재선 길이보다
 * 하나 작으면 마지막이라고 본다. 지금 결재의 step 이 아니라 <b>문서에서 가장 큰
 * step</b> 을 본다는 점이 함정이다 — 같은 문서에 결재가 여러 벌 있으면 뒤엉킨다.
 *
 * @param step 가장 큰 결재 차례
 * @param route 결재선. 사번을 {@code ;} 로 이었다
 */
public record ProposalLastStep(int step, String route) {

    /**
     * 이 차례가 결재선의 마지막인지 본다.
     *
     * @return 마지막 차례면 {@code true}
     */
    public boolean isFinal() {
        return route != null && step == route.split(";").length - 1;
    }
}
