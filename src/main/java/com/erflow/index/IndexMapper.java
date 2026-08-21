package com.erflow.index;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 메인 화면만 쓰는 조회.
 *
 * <p>게시글·쪽지는 각 도메인의 매퍼를 그대로 쓴다. 결재만 여기 있다 — 메인 화면이
 * 보는 것은 {@code recent_proposal_view} 이고 결재 리스트가 보는
 * {@code proposal_view} 와 조건이 다르다. 결재 매퍼에 두면 두 도메인이 서로를
 * 가리키게 되어 고리가 생긴다.
 */
@Mapper
public interface IndexMapper {

    /**
     * 최근 결재.
     *
     * @param userId 현재 사용자 사번
     * @param start 조회 시작 위치
     * @param count 가져올 건수
     * @return 결재 목록
     */
    List<IndexProposalRow> findRecentProposals(
            @Param("userId") String userId,
            @Param("start") int start,
            @Param("count") int count);
}
