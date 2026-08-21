package com.erflow.layout;

import java.util.List;

/**
 * 화면에 그릴 메뉴 항목 하나.
 *
 * <p>{@code menu} 테이블 한 행에 대응하며, {@code children} 은 조립 과정에서 채워진다.
 * 라벨은 레거시 원문을 그대로 쓴다. 권한 프로그램명과 다를 수 있으며 통일하지 않는다
 * ({@code migration/design/00-decisions.md} 의 D-004).
 *
 * @param menuId 메뉴 식별자
 * @param placement 표시 위치. {@code SIDE} 또는 {@code HEADER}
 * @param parentId 상위 메뉴. 최상위면 {@code null}
 * @param sortOrder 같은 부모 안에서의 표시 순서
 * @param label 화면에 보이는 문구
 * @param visibility 표시 조건. {@code ALWAYS} 또는 {@code ADMIN}
 * @param icon 아이콘 class. 글자가 없어 놓치기 쉬운 표시 요소다
 * @param separatorBefore 이 항목 앞에 구분선을 그릴지 여부
 * @param url 클릭 시 이동할 주소. 펼침 전용 그룹이면 {@code null}
 * @param screenId 가리키는 화면. 권한 대상이 아니면 {@code null}
 * @param children 하위 메뉴
 */
public record MenuNode(
        int menuId,
        String placement,
        Integer parentId,
        int sortOrder,
        String label,
        String visibility,
        String icon,
        boolean separatorBefore,
        String url,
        Integer screenId,
        List<MenuNode> children) {

    /** 관리자에게만 보이는 항목의 {@code visibility} 값. */
    public static final String ADMIN_ONLY = "ADMIN";

    /**
     * 하위 메뉴 없이 한 행만 담는 생성자.
     *
     * <p>매퍼가 쓴다. {@code List} 는 타입핸들러가 없어 컬럼으로 매핑할 수 없고,
     * 계층 조립은 어차피 {@link MenuService} 의 일이다.
     *
     * @param menuId 메뉴 식별자
     * @param placement 표시 위치
     * @param parentId 상위 메뉴
     * @param sortOrder 표시 순서
     * @param label 화면에 보이는 문구
     * @param visibility 표시 조건
     * @param icon 아이콘 class
     * @param separatorBefore 앞에 구분선을 그릴지 여부
     * @param url 이동할 주소
     * @param screenId 가리키는 화면
     */
    public MenuNode(
            int menuId,
            String placement,
            Integer parentId,
            int sortOrder,
            String label,
            String visibility,
            String icon,
            boolean separatorBefore,
            String url,
            Integer screenId) {
        this(menuId, placement, parentId, sortOrder, label, visibility,
                icon, separatorBefore, url, screenId, List.of());
    }

    /**
     * 하위 메뉴를 붙인 새 인스턴스를 만든다.
     *
     * @param items 하위 메뉴
     * @return 하위 메뉴가 채워진 복사본
     */
    public MenuNode withChildren(List<MenuNode> items) {
        return new MenuNode(
                menuId, placement, parentId, sortOrder, label, visibility,
                icon, separatorBefore, url, screenId, items);
    }

    /**
     * 펼침 전용 그룹인지 여부.
     *
     * @return 이동할 주소가 없으면 {@code true}
     */
    public boolean isGroup() {
        return url == null || url.isBlank();
    }

    /**
     * 관리자에게만 보이는 항목인지 여부.
     *
     * @return {@code visibility} 가 {@code ADMIN} 이면 {@code true}
     */
    public boolean isAdminOnly() {
        return ADMIN_ONLY.equals(visibility);
    }
}
