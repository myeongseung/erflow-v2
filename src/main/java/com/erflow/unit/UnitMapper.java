package com.erflow.unit;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 설비 매퍼.
 *
 * <p>SQL 은 {@code resources/mapper/unit/UnitMapper.xml} 에 있고, 각 구문에 레거시
 * 출처를 주석으로 달아 뒀다.
 */
@Mapper
public interface UnitMapper {

    /**
     * 목록을 페이지 단위로 조회한다.
     *
     * @param search 검색 조건
     * @param start 조회 시작 위치
     * @param count 가져올 건수
     * @return 설비 목록
     */
    List<UnitRow> findPage(
            @Param("search") UnitSearch search,
            @Param("start") int start,
            @Param("count") int count);

    /**
     * 검색 조건에 걸리는 전체 건수를 센다.
     *
     * @param search 검색 조건
     * @return 전체 건수
     */
    int countBy(@Param("search") UnitSearch search);

    /**
     * 설비 한 건을 읽는다.
     *
     * @param id 장비ID
     * @return 설비. 없으면 {@code null}
     */
    Unit findById(@Param("id") String id);

    /**
     * 설비를 등록한다.
     *
     * @param unit 등록할 설비
     * @return 반영된 행 수
     */
    int insert(@Param("unit") Unit unit);

    /**
     * 설비를 수정한다.
     *
     * @param unit 수정할 설비
     * @return 반영된 행 수
     */
    int update(@Param("unit") Unit unit);

    /**
     * 설비를 삭제한다.
     *
     * @param id 장비ID
     * @return 반영된 행 수
     */
    int delete(@Param("id") String id);
}
