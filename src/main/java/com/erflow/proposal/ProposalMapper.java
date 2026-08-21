package com.erflow.proposal;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 결재 매퍼.
 *
 * <p>SQL 은 {@code resources/mapper/proposal/ProposalMapper.xml} 에 있고, 각 구문에
 * 레거시 출처를 주석으로 달아 뒀다.
 */
@Mapper
public interface ProposalMapper {

    /**
     * 결재 리스트 한 페이지.
     *
     * <p>내가 결재선에 든 진행중 결재({@code result = 3})와, 내가 기안한 결재 중 선택한
     * 상태({@code result}) 를 함께 보여준다 — 레거시 인박스 로직 그대로다.
     *
     * @param userId 현재 사용자 사번
     * @param result 상태 필터(3 결재진행중 / 1 승인 / 2 반려)
     * @param start 조회 시작 위치
     * @param count 가져올 건수
     * @return 결재 목록
     */
    List<ProposalRow> findPage(
            @Param("userId") String userId,
            @Param("result") int result,
            @Param("start") int start,
            @Param("count") int count);

    /**
     * 조건에 걸리는 결재 수.
     *
     * @param userId 현재 사용자 사번
     * @param result 상태 필터
     * @return 전체 건수
     */
    int countBy(@Param("userId") String userId, @Param("result") int result);

    /**
     * 결재 한 건을 넣는다. result 3(진행중)·received_at now 는 고정이다.
     *
     * @param documentId 문서번호
     * @param userId 이 차례를 맡을 사번
     * @param routeId 결재라인 번호
     * @param step 결재 차례. 등록은 0, 승인 뒤 다음 차례는 이전 step + 1
     * @return 반영된 행 수
     */
    int insert(
            @Param("documentId") long documentId,
            @Param("userId") String userId,
            @Param("routeId") int routeId,
            @Param("step") int step);

    /**
     * 방금 넣은 결재의 자동 생성 id. 같은 트랜잭션에서 부른다.
     *
     * @return {@code LAST_INSERT_ID()}
     */
    long lastInsertId();

    /**
     * 결재 한 건을 뷰에서 읽는다. 문서 상세가 쓰는 칸까지 딸려 온다.
     *
     * @param proposalId 결재번호
     * @return 결재. 없으면 {@code null}
     */
    ProposalView findView(@Param("proposalId") long proposalId);

    /**
     * 한 문서에 달린 결재 전부. 결재선을 따라간 자취다.
     *
     * @param documentId 문서번호
     * @return 결재 목록. DB 가 돌려주는 순서 그대로다
     */
    List<ProposalView> findViewsByDocument(@Param("documentId") long documentId);

    /**
     * 한 문서에서 가장 멀리 간 차례.
     *
     * @param documentId 문서번호
     * @return 차례와 결재선. 결재가 없으면 {@code null}
     */
    ProposalLastStep findLastStep(@Param("documentId") long documentId);

    /**
     * 승인/반려가 손댈 결재 한 건.
     *
     * @param proposalId 결재번호
     * @return 결재. 없으면 {@code null}
     */
    ProposalTarget findTarget(@Param("proposalId") long proposalId);

    /**
     * 내 차례를 승인으로 닫는다. 승인 시각이 찍히고 상태가 0 이 된다.
     *
     * @param proposalId 결재번호
     * @param comment 결재 의견
     * @return 반영된 행 수
     */
    int confirm(@Param("proposalId") long proposalId, @Param("comment") String comment);

    /**
     * 문서의 결재 전부를 승인으로 바꾼다. 마지막 차례가 승인했을 때만 부른다.
     *
     * @param documentId 문서번호
     * @return 반영된 행 수
     */
    int confirmAll(@Param("documentId") long documentId);

    /**
     * 반려 의견을 남긴다. 승인과 달리 시각을 찍지 않는다.
     *
     * @param proposalId 결재번호
     * @param comment 반려 의견
     * @return 반영된 행 수
     */
    int reject(@Param("proposalId") long proposalId, @Param("comment") String comment);

    /**
     * 문서의 결재 전부를 반려로 바꾼다.
     *
     * @param documentId 문서번호
     * @return 반영된 행 수
     */
    int rejectAll(@Param("documentId") long documentId);
}
