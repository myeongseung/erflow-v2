package com.erflow.layout;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 메뉴 조회 매퍼.
 *
 * <p>SQL 은 {@code resources/mapper/layout/MenuMapper.xml} 에 있다. 애너테이션 SQL 을
 * 쓰지 않는 이유는 취향이 아니라, 이관한 쿼리가 레거시의 어느 SQL 에서 왔는지 한곳에서
 * 대조할 수 있어야 하기 때문이다.
 */
@Mapper
public interface MenuMapper {

    /**
     * 지정한 위치의 메뉴를 표시 순서대로 평면 조회한다.
     *
     * <p>계층 조립은 {@link MenuService} 가 한다. 재귀 CTE 로 DB 에서 트리를 만들 수도
     * 있지만, 메뉴는 수십 건이고 애플리케이션에서 조립하는 편이 읽기 쉽다.
     *
     * @param placement {@code SIDE} 또는 {@code HEADER}
     * @return 부모 우선, 같은 부모 안에서는 {@code sort_order} 순
     */
    List<MenuNode> findByPlacement(@Param("placement") String placement);
}
