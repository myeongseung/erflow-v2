package com.erflow.proposal;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 결재라인 매퍼.
 *
 * <p>SQL 은 {@code resources/mapper/proposal/ProposalRouteMapper.xml} 에 있고, 각
 * 구문에 레거시 출처를 주석으로 달아 뒀다.
 *
 * <p>결재 도메인은 아직 이관 전이다. 찾기 팝업이 쓰는 조회 하나만 있다.
 */
@Mapper
public interface ProposalRouteMapper {

    /**
     * 내가 만든 결재라인.
     *
     * @param userId 사번
     * @return 결재라인 목록. 번호 순이다
     */
    List<ProposalRouteRow> findByUser(@Param("userId") String userId);

    /**
     * 내 결재라인을 검색·페이지 단위로 조회한다.
     *
     * @param userId 사번
     * @param search 검색 조건
     * @param start 조회 시작 위치
     * @param count 가져올 건수
     * @return 결재라인 목록(가공 전)
     */
    List<ProposalRouteListRow> findPage(
            @Param("userId") String userId,
            @Param("search") ProposalRouteSearch search,
            @Param("start") int start,
            @Param("count") int count);

    /**
     * 검색 조건에 걸리는 내 결재라인 수.
     *
     * @param userId 사번
     * @param search 검색 조건
     * @return 전체 건수
     */
    int countBy(@Param("userId") String userId, @Param("search") ProposalRouteSearch search);

    /**
     * 결재라인 한 건을 넣는다.
     *
     * @param userId 만든 사람 사번
     * @param nickname 결재라인명
     * @param route 사번을 {@code ;} 로 이은 결재 순서
     * @return 반영된 행 수
     */
    int insertRoute(
            @Param("userId") String userId,
            @Param("nickname") String nickname,
            @Param("route") String route);

    /**
     * 수정 화면용으로 결재라인 한 건을 읽는다.
     *
     * @param id 결재관리번호
     * @return 결재라인. 없으면 {@code null}
     */
    ProposalRouteEdit findForUpdate(@Param("id") int id);

    /**
     * 결재라인을 수정한다. 만든 사람·생성 시간은 건드리지 않는다.
     *
     * @param id 결재관리번호
     * @param nickname 결재라인명
     * @param route 사번을 {@code ;} 로 이은 결재 순서
     * @return 반영된 행 수
     */
    int updateRoute(
            @Param("id") int id,
            @Param("nickname") String nickname,
            @Param("route") String route);

    /**
     * 결재라인 한 건을 지운다.
     *
     * @param id 결재관리번호
     * @return 반영된 행 수
     */
    int deleteById(@Param("id") int id);
}
