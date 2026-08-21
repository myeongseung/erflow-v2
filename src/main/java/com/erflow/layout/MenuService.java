package com.erflow.layout;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 메뉴 트리를 조립한다.
 *
 * <p>레거시는 메뉴가 {@code indexSide.jsp} 에 하드코딩돼 145개 화면에 include 됐다.
 * 이제는 테이블 한 벌에서 읽으므로 화면 템플릿이 메뉴를 알 필요가 없다.
 * 설계는 {@code migration/design/01-menu-layout.md}.
 */
@Service
public class MenuService {

    /** 사이드바 위치 값. */
    public static final String SIDE = "SIDE";

    /** 헤더 위치 값. */
    public static final String HEADER = "HEADER";

    private final MenuMapper menuMapper;

    /**
     * @param menuMapper 메뉴 조회 매퍼
     */
    public MenuService(MenuMapper menuMapper) {
        this.menuMapper = menuMapper;
    }

    /**
     * 지정한 위치의 메뉴를 계층으로 돌려준다.
     *
     * <p>표시 순서는 레거시 마크업의 순서를 그대로 따른다. 정렬을 바꾸면 사용자가 보던
     * 화면이 달라지므로 {@code sort_order} 를 신뢰하고 재정렬하지 않는다.
     *
     * @param placement {@link #SIDE} 또는 {@link #HEADER}
     * @param admin 관리자 여부. {@code false} 면 관리자 전용 항목을 제외한다
     * @return 최상위 메뉴 목록. 각 항목이 하위 메뉴를 갖는다
     */
    @Transactional(readOnly = true)
    public List<MenuNode> tree(String placement, boolean admin) {
        List<MenuNode> rows = menuMapper.findByPlacement(placement);

        Map<Integer, List<MenuNode>> byParent = new LinkedHashMap<>();
        List<MenuNode> roots = new ArrayList<>();
        for (MenuNode row : rows) {
            if (!admin && row.isAdminOnly()) {
                continue;
            }
            if (row.parentId() == null) {
                roots.add(row);
            } else {
                byParent.computeIfAbsent(row.parentId(), key -> new ArrayList<>()).add(row);
            }
        }

        List<MenuNode> tree = new ArrayList<>(roots.size());
        for (MenuNode root : roots) {
            tree.add(root.withChildren(byParent.getOrDefault(root.menuId(), List.of())));
        }
        return tree;
    }

    /**
     * 사이드바 메뉴를 돌려준다.
     *
     * @param admin 관리자 여부
     * @return 사이드바 메뉴 트리
     */
    @Transactional(readOnly = true)
    public List<MenuNode> sideMenu(boolean admin) {
        return tree(SIDE, admin);
    }

    /**
     * 헤더 메뉴를 돌려준다.
     *
     * @param admin 관리자 여부
     * @return 헤더 메뉴 트리
     */
    @Transactional(readOnly = true)
    public List<MenuNode> headerMenu(boolean admin) {
        return tree(HEADER, admin);
    }
}
