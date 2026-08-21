package com.erflow.proposal;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결재라인 찾기 팝업이 쓰는 조회.
 *
 * <p>결재 등록 화면이 «결재라인 불러오기»로 여는 팝업이다. 남이 만든 라인은 보이지
 * 않는다 — 조회가 사번으로 좁혀져 있다.
 */
@Service
public class ProposalRouteFinder {

    private final ProposalRouteMapper proposalRouteMapper;

    /**
     * @param proposalRouteMapper 결재라인 매퍼
     */
    public ProposalRouteFinder(ProposalRouteMapper proposalRouteMapper) {
        this.proposalRouteMapper = proposalRouteMapper;
    }

    /**
     * 검색어에 걸리는 내 결재라인.
     *
     * <p>이름만 훑는다. 번호로는 찾지 못한다 — 협력업체 팝업과 같은 모양이며(D-039)
     * 이 화면은 도움말까지 그쪽에서 복사해 왔다.
     *
     * <p>이름이 겹치는 결재라인이 실제로 있다. 레거시는 이름으로만 정렬하고 겹치는
     * 둘의 앞뒤를 {@code HashMap} 순서에 맡기는데, 지금 데이터에서는 그 순서가 번호
     * 순과 같다. 번호 순으로 읽어 온 목록을 이름으로 안정 정렬해 같은 결과를 낸다.
     *
     * @param userId 로그인한 사람의 사번
     * @param search 검색어. {@code null} 이면 빈 문자열로 본다
     * @return 이름 순으로 정렬한 결재라인 목록
     */
    @Transactional(readOnly = true)
    public List<ProposalRouteRow> search(String userId, String search) {
        String needle = search == null ? "" : search;

        return proposalRouteMapper.findByUser(userId).stream()
                .filter(route -> needle.isEmpty() || route.nickname().contains(needle))
                .sorted(Comparator.comparing(ProposalRouteRow::nickname))
                .toList();
    }
}
