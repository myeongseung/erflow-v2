package com.erflow.layout;

import com.erflow.auth.ErflowUserDetails;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 모든 화면에 레이아웃 데이터를 넣어 준다.
 *
 * <p>레거시는 화면마다 {@code <%@include file="/indexHeader.jsp"%>} 를 적었고, 그 안에서
 * 세션을 직접 뒤졌다. 이제는 화면 컨트롤러가 레이아웃을 알 필요가 없다.
 */
@ControllerAdvice
public class LayoutModelAdvice {

    private final MenuService menuService;

    /**
     * @param menuService 메뉴 조회 서비스
     */
    public LayoutModelAdvice(MenuService menuService) {
        this.menuService = menuService;
    }

    /**
     * 사이드 메뉴.
     *
     * @return 사이드바 메뉴 트리
     */
    @ModelAttribute("sideMenu")
    public List<MenuNode> sideMenu() {
        return menuService.sideMenu(isAdmin());
    }

    /**
     * 헤더 드롭다운 메뉴.
     *
     * @return 헤더 메뉴 트리
     */
    @ModelAttribute("headerMenu")
    public List<MenuNode> headerMenu() {
        return menuService.headerMenu(isAdmin());
    }

    /**
     * 헤더에 표시할 사용자 문구.
     *
     * <p>레거시 {@code indexHeader.jsp} 는 {@code headerUserId + headerUserName} 을 찍었다.
     *
     * @return 화면에 보일 사용자 표기
     */
    @ModelAttribute("currentUser")
    public String currentUser() {
        ErflowUserDetails user = principal();
        return user == null ? "" : user.display();
    }

    /**
     * 관리자 여부.
     *
     * <p>레거시 {@code PermissionController.isAdmin(session)} 에 대응한다.
     * 부서·직급 권한 양쪽에 최상위 비트가 켜져 있어야 한다.
     *
     * @return 관리자면 {@code true}
     */
    private boolean isAdmin() {
        ErflowUserDetails user = principal();
        return user != null && user.admin();
    }

    private static ErflowUserDetails principal() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.getPrincipal() instanceof ErflowUserDetails user) {
            return user;
        }
        return null;
    }
}
