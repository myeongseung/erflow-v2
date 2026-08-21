package com.erflow.proposal;

import java.util.List;

/**
 * 문서 상세 화면 한 벌.
 *
 * <p>레거시는 이 계산을 전부 {@code proposalDocument.jsp} 스크립틀릿에서 했다 —
 * 결재선을 쪼개고, 결재자 이름을 하나씩 조회하고, 날짜 형식을 바꿨다. 화면에서
 * 걷어내 여기 모은다.
 *
 * @param id 결재번호. 승인/반려 폼이 돌려보낸다
 * @param subject 문서제목
 * @param content 문서 내용(HTML 그대로 나간다)
 * @param step 이 결재의 차례. 0(기안자 자리)이면 반려 버튼이 없다
 * @param editable 결재할 수 있는 상태인가(진행중이거나 내 차례가 끝난 것). 의견 입력과
 *     버튼이 보일지를 가른다
 * @param reviewCells 결재선 머리글의 «검토» 칸. 값은 쓰지 않고 개수만 쓴다 —
 *     결재선 길이에서 «담당»·«승인» 을 뺀 만큼이며, 모자라면 비어 있다
 * @param stamps 결재선 자리마다의 도장
 * @param comments 결재 의견 목록
 */
public record ProposalDocument(
        long id,
        String subject,
        String content,
        int step,
        boolean editable,
        List<Integer> reviewCells,
        List<ProposalStamp> stamps,
        List<ProposalComment> comments) {
}
