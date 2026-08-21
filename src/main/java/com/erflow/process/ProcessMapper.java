package com.erflow.process;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 공정 매퍼.
 *
 * <p>SQL 은 {@code resources/mapper/process/ProcessMapper.xml} 에 있다. 삭제만 저장
 * 프로시저를 부른다 — 레거시가 그렇다(D-073).
 */
@Mapper
public interface ProcessMapper {

    /**
     * 공정 목록 한 페이지.
     *
     * @param search 검색 조건
     * @param start 조회 시작 위치
     * @param count 가져올 건수
     * @return 공정 목록. 공정ID 순이다
     */
    List<ProcessRow> findPage(
            @Param("search") ProcessSearch search,
            @Param("start") int start,
            @Param("count") int count);

    /**
     * 조건에 걸리는 공정 수.
     *
     * @param search 검색 조건
     * @return 건수
     */
    int countBy(@Param("search") ProcessSearch search);

    /**
     * 공정 한 건.
     *
     * @param id 공정ID
     * @return 공정. 없으면 {@code null}
     */
    ProcessRow findById(@Param("id") String id);

    /**
     * 공정을 넣는다.
     *
     * @param process 넣을 공정
     * @return 반영된 행 수
     */
    int insertProcess(ProcessRow process);

    /**
     * 공정명을 고친다. 고리와 우선순위는 건드리지 않는다.
     *
     * @param id 공정ID
     * @param name 새 이름
     * @return 반영된 행 수
     */
    int updateName(@Param("id") String id, @Param("name") String name);

    /**
     * 공정을 지운다. 저장 프로시저를 부른다.
     *
     * @param id 공정ID
     * @return 반영된 행 수
     */
    int callDeleteProcess(@Param("id") String id);
}
