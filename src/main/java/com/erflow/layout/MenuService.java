package com.erflow.layout;

import com.erflow.auth.Permissions;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 메뉴 트리 — 사이드바와 헤더.
 *
 * <p>들어갈 수 없는 항목은 그리지 않는다(D-135). 표시와 접근이 같은 값 하나다.
 *
 * <p>메뉴 행과 프로그램 권한은 <b>메모리에 한 벌</b> 둔다(D-152). 화면마다 DB 에 네 번씩
 * 가던 것을 없앤다. 권한을 바꾸는 쪽({@code AdminPermissionService})이
 * {@link #refresh()} 를 부르고, 부르지 않은 경로가 있어도 {@link #MAX_AGE} 뒤에는
 * 다시 읽는다.
 */
@Service
public class MenuService {

    /** 사이드바 메뉴의 placement 값. */
    public static final String SIDE = "SIDE";

    /** 헤더 메뉴의 placement 값. */
    public static final String HEADER = "HEADER";

    /** 다시 읽지 않고 쓰는 최대 시간. */
    static final Duration MAX_AGE = Duration.ofSeconds(30);

    private final MenuMapper menuMapper;

    private volatile Snapshot snapshot;

    /**
     * @param menuMapper 메뉴 매퍼
     */
    public MenuService(MenuMapper menuMapper) {
        this.menuMapper = menuMapper;
    }

    /**
     * 메모리의 메뉴 한 벌을 버린다. 다음 화면이 DB 에서 다시 읽는다.
     *
     * <p>프로그램 권한이나 부서·직급을 바꾼 뒤에 부른다.
     */
    public void refresh() {
        snapshot = null;
    }

    /**
     * 한 자리(사이드바 또는 헤더)의 메뉴 트리.
     *
     * @param placement {@link #SIDE} 또는 {@link #HEADER}
     * @param admin 관리자면 {@code true} — 관리자 전용 항목이 보인다
     * @param deptPermission 부서 권한 비트
     * @param jobPermission 직급 권한 비트
     * @return 들어갈 수 있는 항목만 남긴 트리
     */
    public List<MenuNode> tree(
            String placement, boolean admin, long deptPermission, long jobPermission) {
        Snapshot current = snapshot();
        List<MenuNode> rows = SIDE.equals(placement) ? current.side() : current.header();
        Map<Integer, MenuAccess> accessByScreen = current.access();

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
     * 사이드바 메뉴.
     *
     * @param admin 관리자 여부
     * @param deptPermission 부서 권한 비트
     * @param jobPermission 직급 권한 비트
     * @return 사이드바 트리
     */
    public List<MenuNode> sideMenu(boolean admin, long deptPermission, long jobPermission) {
        return tree(SIDE, admin, deptPermission, jobPermission);
    }

    /**
     * 헤더 메뉴.
     *
     * @param admin 관리자 여부
     * @param deptPermission 부서 권한 비트
     * @param jobPermission 직급 권한 비트
     * @return 헤더 트리
     */
    public List<MenuNode> headerMenu(boolean admin, long deptPermission, long jobPermission) {
        return tree(HEADER, admin, deptPermission, jobPermission);
    }

    /** 메모리의 한 벌. 없거나 오래됐으면 DB 에서 읽는다 — 둘이 동시에 읽어도 같은 값이다. */
    private Snapshot snapshot() {
        Snapshot current = snapshot;
        if (current == null || current.stale()) {
            current = load();
            snapshot = current;
        }
        return current;
    }

    private Snapshot load() {
        Map<Integer, MenuAccess> access = new HashMap<>();
        for (MenuAccess row : menuMapper.findMenuAccess()) {
            access.put(row.screenId(), row);
        }
        return new Snapshot(
                List.copyOf(menuMapper.findByPlacement(SIDE)),
                List.copyOf(menuMapper.findByPlacement(HEADER)),
                Map.copyOf(access),
                Instant.now());
    }

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

    /**
     * 메뉴 한 벌 — 읽은 시각과 함께.
     *
     * @param side 사이드바 행들
     * @param header 헤더 행들
     * @param access 화면 번호 → 요구 권한
     * @param readAt 읽은 시각
     */
    private record Snapshot(
            List<MenuNode> side,
            List<MenuNode> header,
            Map<Integer, MenuAccess> access,
            Instant readAt) {

        boolean stale() {
            return readAt.plus(MAX_AGE).isBefore(Instant.now());
        }
    }
}
