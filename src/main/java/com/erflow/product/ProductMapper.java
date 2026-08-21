package com.erflow.product;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 제품 매퍼.
 *
 * <p>SQL 은 {@code resources/mapper/product/ProductMapper.xml} 에 있고, 각 구문에
 * 레거시 출처를 주석으로 달아 뒀다.
 *
 * <p>제품 도메인은 아직 이관 전이다. 찾기 팝업이 쓰는 조회 하나만 있다.
 */
@Mapper
public interface ProductMapper {

    /**
     * 찾기 팝업이 쓰는 (코드, 이름) 목록.
     *
     * @return 모든 제품
     */
    List<ProductRow> findAllNames();

    /**
     * 분류별 제품 목록 한 페이지.
     *
     * @param type 분류 코드
     * @param search 검색 조건
     * @param start 조회 시작 위치
     * @param count 가져올 건수
     * @return 제품 목록. 제품 ID 순이다
     */
    List<ProductListRow> findPage(
            @Param("type") int type,
            @Param("search") ProductSearch search,
            @Param("start") int start,
            @Param("count") int count);

    /**
     * 조건에 걸리는 제품 수.
     *
     * @param type 분류 코드
     * @param search 검색 조건
     * @return 건수
     */
    int countBy(@Param("type") int type, @Param("search") ProductSearch search);

    /**
     * 제품 한 건.
     *
     * @param id 제품 ID
     * @return 제품. 없으면 {@code null}
     */
    ProductListRow findById(@Param("id") String id);

    /**
     * 제품을 넣는다.
     *
     * @param id 제품 ID
     * @param name 이름
     * @param count 수량
     * @param type 분류 코드
     * @return 반영된 행 수
     */
    int insertProduct(
            @Param("id") String id,
            @Param("name") String name,
            @Param("count") int count,
            @Param("type") int type);

    /**
     * 제품을 고친다. 제품 ID 는 바뀌지 않는다.
     *
     * @param id 제품 ID
     * @param name 이름
     * @param count 수량
     * @param type 분류 코드
     * @return 반영된 행 수
     */
    int updateProduct(
            @Param("id") String id,
            @Param("name") String name,
            @Param("count") int count,
            @Param("type") int type);

    /**
     * 제품을 지운다.
     *
     * @param id 제품 ID
     * @return 반영된 행 수
     */
    int deleteProduct(@Param("id") String id);
}
