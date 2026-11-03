package com.erflow.layout;

import com.erflow.auth.ErflowUserDetails;
import com.erflow.message.MessageMapper;
import com.erflow.user.UserRow;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 모든 화면에 레이아웃 데이터를 넣어 준다.
 *
 * <p>화면 컨트롤러는 레이아웃을 알 필요가 없다 — 사이드 메뉴·헤더 메뉴·로그인한
 * 사람·안 읽은 쪽지 수·지금 있는 관리자 구역이 전부 여기서 들어간다(D-150).
 */
@ControllerAdvice
public class LayoutModelAdvice {

    private final MenuService menuService;

    private final MessageMapper messageMapper;

    /**
     * @param menuService 메뉴 조회 서비스
     * @param messageMapper 쪽지 조회 (안 읽은 수)
     */
    public LayoutModelAdvice(MenuService menuService, MessageMapper messageMapper) {
        this.menuService = menuService;
        this.messageMapper = messageMapper;
    }

    /**
     * 사이드 메뉴.
     *
     * <p>들어갈 수 없는 항목은 빠진다(D-135). 로그인 전(권한 0)이면 권한 대상 메뉴가
     * 전부 빠지는데, 그 화면들(로그인·오류)은 어차피 사이드바를 그리지 않는다.
     *
     * @return 사이드바 메뉴 트리
     */
    @ModelAttribute("sideMenu")
    public List<MenuNode> sideMenu() {
        return menuService.sideMenu(isAdmin(), deptPermission(), jobPermission());
    }

    /**
     * 헤더 드롭다운 메뉴.
     *
     * @return 헤더 메뉴 트리
     */
    @ModelAttribute("headerMenu")
    public List<MenuNode> headerMenu() {
        return menuService.headerMenu(isAdmin(), deptPermission(), jobPermission());
    }

    /**
     * 헤더에 표시할 사용자 문구 ("사번 이름").
     *
     * @return 화면에 보일 사용자 표기
     */
    @ModelAttribute("currentUser")
    public String currentUser() {
        ErflowUserDetails user = principal();
        return user == null ? "" : user.display();
    }

    /**
     * 로그인한 사람 — 이름·부서·직급 (D-150). 사이드바의 사람 카드와 헤더의
     * 머리글자 단추가 쓴다.
     *
     * <p>DB 에 가지 않는다 — 로그인 때 읽어 세션에 둔 값이다(D-152).
     *
     * @return 사원 한 줄. 로그인 전이면 {@code null}
     */
    @ModelAttribute("me")
    public UserRow me() {
        ErflowUserDetails user = principal();
        return user == null ? null
                : new UserRow(user.deptName(), user.jobName(), user.id(), user.name());
    }

    /**
     * 안 읽은 쪽지 수 (D-150). 헤더의 종에 배지로 붙는다.
     *
     * @return 받은 쪽지 중 아직 안 읽은 건수. 로그인 전이면 0
     */
    @ModelAttribute("unreadMessages")
    public int unreadMessages() {
        ErflowUserDetails user = principal();
        return user == null ? 0 : messageMapper.countUnread(user.id());
    }

    /**
     * 지금 있는 사용자 메뉴의 주소 (D-159). 사용자 사이드바가 현재 항목을 밝힌다.
     *
     * <p>요청 주소(경로 + 물음표 뒤)로 시작하는 메뉴 주소 가운데 가장 긴 것을 고른다.
     * 없으면 첫 경로 조각이 같은 메뉴(예: /post/view 는 /post/board-list)로 한다.
     *
     * @param request 현재 요청
     * @return 현재 항목의 메뉴 주소. 맞는 것이 없으면 {@code null}
     */
    @ModelAttribute("currentMenuUrl")
    public String currentMenuUrl(HttpServletRequest request) {
        String current = request.getRequestURI()
                + (request.getQueryString() == null ? "" : "?" + request.getQueryString());
        List<String> urls = new java.util.ArrayList<>();
        for (MenuNode root : sideMenu()) {
            if (!root.isGroup()) {
                urls.add(root.url());
            }
            for (MenuNode child : root.children()) {
                if (!child.isGroup()) {
                    urls.add(child.url());
                }
            }
        }
        String best = null;
        for (String url : urls) {
            if (current.startsWith(url) && (best == null || url.length() > best.length())) {
                best = url;
            }
        }
        if (best != null) {
            return best;
        }
        String head = firstSegment(request.getRequestURI());
        for (String url : urls) {
            if (url.equals(head) || url.startsWith(head + "/") || url.startsWith(head + "?")) {
                return url;
            }
        }
        return null;
    }

    /** {@code /post/list} → {@code /post}. */
    private static String firstSegment(String path) {
        int second = path.indexOf('/', 1);
        return second < 0 ? path : path.substring(0, second);
    }

    /**
     * 지금 있는 관리자 구역 (D-150). 관리자 사이드바가 현재 항목을 밝힌다.
     *
     * <p>주소의 앞부분으로 가른다 — 한 구역의 목록·추가·수정이 같은 항목에 속한다.
     *
     * @param request 현재 요청
     * @return home · user · jobdept · menu · document · board. 관리자 밖이면 {@code null}
     */
    @ModelAttribute("adminArea")
    public String adminArea(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.startsWith("/admin/user")) {
            return "user";
        }
        if (path.startsWith("/admin/permission/menu")
                || path.startsWith("/admin/permission/program")) {
            return "menu";
        }
        if (path.startsWith("/admin/permission")) {
            return "jobdept";
        }
        if (path.startsWith("/admin/document")) {
            return "document";
        }
        if (path.startsWith("/admin/board")) {
            return "board";
        }
        if (path.startsWith("/admin")) {
            return "home";
        }
        return null;
    }

    /**
     * 관리자 여부.
     *
     * <p>부서·직급 권한 양쪽에 최상위 비트가 켜져 있어야 한다.
     *
     * @return 관리자면 {@code true}
     */
    private boolean isAdmin() {
        ErflowUserDetails user = principal();
        return user != null && user.admin();
    }

    private long deptPermission() {
        ErflowUserDetails user = principal();
        return user == null ? 0L : user.deptPermission();
    }

    private long jobPermission() {
        ErflowUserDetails user = principal();
        return user == null ? 0L : user.jobPermission();
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
