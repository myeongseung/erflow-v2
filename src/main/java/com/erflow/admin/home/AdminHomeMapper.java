package com.erflow.admin.home;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 관리자 대시보드 매퍼.
 *
 * <p>SQL 은 {@code resources/mapper/admin/AdminHomeMapper.xml} 에 있다.
 */
@Mapper
public interface AdminHomeMapper {

    /**
     * 최근 결재. 화면은 이 목록을 그대로 다 찍는다.
     *
     * @return 결재 목록. 번호 내림차순
     */
    List<RecentProposal> findRecentProposals();

    /**
     * 최근 수·발주 15건. 수주와 발주가 함께 나온다.
     *
     * @return 수·발주 목록
     */
    List<RecentTask> findRecentTasks();

    /**
     * 그날의 근무 기록.
     *
     * @param date {@code yyyy-MM-dd}
     * @return 근무 목록. 사번순
     */
    List<WorkRow> findWorks(@Param("date") String date);

    /**
     * 사원 수 — 사원 관리 목록의 «전체 N명» 과 같은 셈이다 (D-151). 관리자 계정은 뺀다.
     *
     * @return user_view 의 행 수 (관리자 제외)
     */
    int countUsers();

    /**
     * 결재가 돌고 있는 문서 수 (D-151). 진행 중(result 3)인 단계를 가진 문서를 센다.
     *
     * @return 미결 문서 수
     */
    int countRunningDocuments();

    /**
     * 최근 며칠 동안 올라온 글 수 (D-151). 지운 글은 빼고, 모든 게시판을 합친다.
     *
     * @param days 거슬러 올라갈 날수
     * @return 글 수
     */
    int countPostsSince(@Param("days") int days);
}
