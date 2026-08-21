package com.erflow.proposal;

/**
 * 문서 상세 오른쪽에 쌓이는 결재 의견 한 줄.
 *
 * <p>결재 행마다 하나씩 달리지만 <b>의견이 빈 것은 줄을 만들지 않는다</b>. 레거시가
 * 그렇게 걸러 낸다.
 *
 * @param userName 결재자 이름
 * @param comment 의견
 */
public record ProposalComment(String userName, String comment) {
}
