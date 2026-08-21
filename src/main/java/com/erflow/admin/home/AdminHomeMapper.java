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
}
