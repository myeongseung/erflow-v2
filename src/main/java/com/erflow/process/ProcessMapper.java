package com.erflow.process;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 공정 매퍼.
 *
 * <p>SQL 은 {@code resources/mapper/process/ProcessMapper.xml} 에 있다. 레거시는
 * 삭제만 저장 프로시저({@code DeleteProcess})에 맡겼는데 그 정의를 볼 수 없었다
 * (D-073). 이제 삭제도 우리가 만든 SQL 이다 — 고리 잇기는 {@link ProcessService}
 * 가 한다(D-133).
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
     * 공정의 다음 고리를 바꾼다.
     *
     * @param id 공정ID
     * @param nextId 새 다음 공정ID. 마지막이 되면 {@code null}
     * @return 반영된 행 수
     */
    int updateNextOf(@Param("id") String id, @Param("nextId") String nextId);

    /**
     * 공정의 이전 고리를 바꾼다.
     *
     * @param id 공정ID
     * @param prevId 새 이전 공정ID. 첫 공정이 되면 {@code null}
     * @return 반영된 행 수
     */
    int updatePrevOf(@Param("id") String id, @Param("prevId") String prevId);

    /**
     * 공정의 자리 번호를 한 칸 앞으로 당긴다.
     *
     * @param id 공정ID
     * @return 반영된 행 수
     */
    int decrementPriority(@Param("id") String id);

    /**
     * 공정을 참조하는 다른 기록의 수 — 관리·생산 기록.
     *
     * <p>참조가 남아 있는 공정을 지우면 그 기록들이 없는 공정을 가리키게 된다.
     * 지우기 전에 세어 보고, 있으면 지우지 않는다(D-133).
     *
     * @param id 공정ID
     * @return 참조 건수
     */
    int countReferences(@Param("id") String id);

    /**
     * 공정 한 행을 지운다.
     *
     * @param id 공정ID
     * @return 반영된 행 수
     */
    int deleteById(@Param("id") String id);
}
