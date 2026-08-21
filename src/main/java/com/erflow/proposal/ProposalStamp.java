package com.erflow.proposal;

/**
 * 결재 스탬프 표의 한 칸. 결재선 자리 하나에 해당한다.
 *
 * <p>이름과 날짜는 <b>승인된 자리에만</b> 찍힌다. 아직 오지 않은 차례는 칸만 있고 비어
 * 있다. 결재 행이 아예 없는 자리({@code filled} 가 거짓)는 레거시가 {@code &nbsp;&nbsp;}
 * 로 폭만 잡아 둔다.
 *
 * @param filled 이 자리에 결재 행이 있는가
 * @param name 결재자 이름. 승인 전이면 빈 문자열
 * @param approvedAt 승인 날짜({@code MM/dd}). 승인 전이면 빈 문자열
 */
public record ProposalStamp(boolean filled, String name, String approvedAt) {
}
