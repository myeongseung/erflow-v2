package com.erflow.work;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 근태 확인이 쓰는 조회.
 *
 * <p>SQL 은 {@code resources/mapper/work/WorkMapper.xml} 에 있고, 각 구문에 레거시
 * 출처를 주석으로 달아 뒀다. 근무 기록 조회는 프로필과 같은 것을 쓴다
 * ({@code ProfileMapper.findWorks}) — 레거시도 같은 {@code getWorkViews} 를 불렀다.
 */
@Mapper
public interface WorkMapper {

    /**
     * 근태 표에 실을 사람들.
     *
     * @param dept 부서 검색어. 비우면 조건이 붙지 않는다
     * @param name 이름 검색어. 비우면 조건이 붙지 않는다
     * @param start 조회 시작 위치
     * @param count 가져올 인원
     * @return 사람 목록. {@code admin} 은 빠진다
     */
    List<WorkUserRow> findUsers(
            @Param("dept") String dept,
            @Param("name") String name,
            @Param("start") int start,
            @Param("count") int count);

    /**
     * 조건에 걸리는 전체 인원.
     *
     * @param dept 부서 검색어
     * @param name 이름 검색어
     * @return 전체 인원
     */
    int countUsers(@Param("dept") String dept, @Param("name") String name);
}
