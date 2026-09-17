package com.erflow.layout;

import com.erflow.auth.Permissions;
import java.util.ArrayList;
import java.util.HashMap;
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
 *
 * <p><b>들어갈 수 없는 메뉴는 그리지 않는다 (D-135).</b> 레거시는 전부 보여주고
 * 클릭한 뒤에야 권한 오류를 냈다. 이제 메뉴가 화면 접근 판정과 같은 값
 * ({@code permission_program_tbl})을 보므로, 보이는 메뉴는 반드시 들어갈 수 있고
 * 못 들어가는 메뉴는 애초에 없다. 하위가 전부 숨은 그룹은 제목도 숨는다.
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
     * <p>화면을 가리키는 항목은 그 화면의 요구 권한을 통과해야 보인다(D-135). 관리자를
     * 따로 우대하지 않는다 — 프로그램 권한 값에 관리자 비트가 늘 들어 있어 관리자는
     * 어차피 통과하고, 권한 행이 없는 화면은 관리자도 못 들어가므로 메뉴도 숨는 것이
     * 옳다. 화면 접근 판정({@code ScreenAuthorizationManager})과 정확히 같은 규칙이다.
     *
     * @param placement {@link #SIDE} 또는 {@link #HEADER}
     * @param admin 관리자 여부. {@code false} 면 관리자 전용 항목을 제외한다
     * @param deptPermission 보는 사람의 부서 권한 비트마스크
     * @param jobPermission 보는 사람의 직급 권한 비트마스크
     * @return 최상위 메뉴 목록. 각 항목이 하위 메뉴를 갖는다
     */
    @Transactional(readOnly = true)
    public List<MenuNode> tree(
            String placement, boolean admin, long deptPermission, long jobPermission) {
        List<MenuNode> rows = menuMapper.findByPlacement(placement);
        Map<Integer, MenuAccess> accessByScreen = new HashMap<>();
        for (MenuAccess access : menuMapper.findMenuAccess()) {
            accessByScreen.put(access.screenId(), access);
        }

        Map<Integer, List<MenuNode>> byParent = new LinkedHashMap<>();
        List<MenuNode> roots = new ArrayList<>();
        for (MenuNode row : rows) {
            if (!admin && row.isAdminOnly()) {
                continue;
            }
            if (!enterable(row, accessByScreen, deptPermission, jobPermission)) {
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
            List<MenuNode> children = byParent.getOrDefault(root.menuId(), List.of());
            if (root.isGroup() && children.isEmpty()) {
                // 하위가 전부 숨은 그룹은 제목만 남는다 — 제목도 숨긴다(D-135).
                continue;
            }
            tree.add(root.withChildren(children));
        }
        return tree;
    }

    /**
     * 사이드바 메뉴를 돌려준다.
     *
     * @param admin 관리자 여부
     * @param deptPermission 보는 사람의 부서 권한
     * @param jobPermission 보는 사람의 직급 권한
     * @return 사이드바 메뉴 트리
     */
    @Transactional(readOnly = true)
    public List<MenuNode> sideMenu(boolean admin, long deptPermission, long jobPermission) {
        return tree(SIDE, admin, deptPermission, jobPermission);
    }

    /**
     * 헤더 메뉴를 돌려준다.
     *
     * @param admin 관리자 여부
     * @param deptPermission 보는 사람의 부서 권한
     * @param jobPermission 보는 사람의 직급 권한
     * @return 헤더 메뉴 트리
     */
    @Transactional(readOnly = true)
    public List<MenuNode> headerMenu(boolean admin, long deptPermission, long jobPermission) {
        return tree(HEADER, admin, deptPermission, jobPermission);
    }

    /**
     * 이 항목이 가리키는 화면에 들어갈 수 있는가.
     *
     * <p>화면을 가리키지 않는 항목(그룹, 로그아웃, 설정)은 권한 대상이 아니라 통과한다 —
     * 그룹은 하위로, 설정은 {@code visibility=ADMIN} 으로 따로 걸러진다.
     */
    private static boolean enterable(
            MenuNode row, Map<Integer, MenuAccess> accessByScreen, long dept, long job) {
        if (row.screenId() == null) {
            return true;
        }
        MenuAccess access = accessByScreen.get(row.screenId());
        long deptLevel = access == null ? 0L : access.deptLevel();
        long jobLevel = access == null ? 0L : access.jobLevel();
        return Permissions.hasProgramPermission(dept, job, deptLevel, jobLevel);
    }
}
