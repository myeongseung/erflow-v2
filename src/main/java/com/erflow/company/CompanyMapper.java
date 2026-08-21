package com.erflow.company;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 협력업체 매퍼.
 *
 * <p>SQL 은 {@code resources/mapper/company/CompanyMapper.xml} 에 있고, 각 구문에
 * 레거시 출처를 주석으로 달아 뒀다.
 */
@Mapper
public interface CompanyMapper {

    /**
     * 목록을 페이지 단위로 조회한다.
     *
     * @param subcontract 0 영업 / 1 구매
     * @param search 검색 조건
     * @param start 조회 시작 위치
     * @param count 가져올 건수
     * @return 협력업체 목록
     */
    List<Company> findPage(
            @Param("subcontract") int subcontract,
            @Param("search") CompanySearch search,
            @Param("start") int start,
            @Param("count") int count);

    /**
     * 검색 조건에 걸리는 전체 건수를 센다.
     *
     * @param subcontract 0 영업 / 1 구매
     * @param search 검색 조건
     * @return 전체 건수
     */
    int countBy(
            @Param("subcontract") int subcontract, @Param("search") CompanySearch search);

    /**
     * 협력업체 한 건을 읽는다.
     *
     * @param id 번호
     * @return 협력업체. 없으면 {@code null}
     */
    Company findById(@Param("id") int id);

    /**
     * 찾기 팝업이 쓰는 (번호, 이름) 목록.
     *
     * @return 모든 협력업체. 목록 화면과 달리 구매·영업을 가리지 않는다
     */
    List<CompanyRow> findAllNames();

    /**
     * 협력업체를 등록한다.
     *
     * @param company 등록할 협력업체
     * @return 반영된 행 수
     */
    int insert(@Param("company") Company company);

    /**
     * 협력업체를 수정한다.
     *
     * @param company 수정할 협력업체
     * @return 반영된 행 수
     */
    int update(@Param("company") Company company);

    /**
     * 협력업체를 삭제한다.
     *
     * @param id 번호
     * @return 반영된 행 수
     */
    int delete(@Param("id") int id);
}
