package com.erflow.auth;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * 로그인한 사용자.
 *
 * <p>레거시는 {@code session.setAttribute("user", UserBean)} 으로 담으면서 비밀번호와
 * 주민번호를 {@code null} 로 지웠다. 여기서는 애초에 담지 않는다.
 */
public class ErflowUserDetails implements UserDetails {

    /** 정상 로그인한 사용자. */
    public static final String ROLE_USER = "ROLE_USER";

    /**
     * 관리자.
     *
     * <p>레거시 {@code PermissionController.isAdmin(session)} 이 참인 사용자다. 관리자
     * 화면은 {@code screen} 테이블에 없어 프로그램 권한으로 막을 수 없다 — 이 권한으로
     * {@code /admin/**} 을 막는다(D-053).
     */
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    /**
     * 비밀번호를 바꿔야만 하는 상태.
     *
     * <p>레거시는 비밀번호가 사번과 같으면 최초 로그인으로 보고 세션에 {@code tempId}
     * 만 담은 채 비밀번호 변경 화면으로 보냈다. 즉 <b>로그인시키지 않았다.</b>
     * 같은 뜻으로, 이 권한만 가진 사용자는 비밀번호 변경 외에 아무것도 할 수 없다.
     */
    public static final String ROLE_PASSWORD_CHANGE = "ROLE_PASSWORD_CHANGE";

    private final String id;
    private final String name;
    private final transient String password;
    private final long deptPermission;
    private final long jobPermission;
    private final boolean passwordChangeRequired;

    /**
     * @param user 조회된 사용자
     * @param passwordChangeRequired 비밀번호가 사번과 같아 변경이 필요한지
     */
    public ErflowUserDetails(AuthUser user, boolean passwordChangeRequired) {
        this.id = user.id();
        this.name = user.name();
        this.password = user.password();
        this.deptPermission = user.deptPermission();
        this.jobPermission = user.jobPermission();
        this.passwordChangeRequired = passwordChangeRequired;
    }

    /**
     * @return 사번
     */
    public String id() {
        return id;
    }

    /**
     * @return 이름
     */
    public String name() {
        return name;
    }

    /**
     * @return 부서 권한 비트마스크
     */
    public long deptPermission() {
        return deptPermission;
    }

    /**
     * @return 직급 권한 비트마스크
     */
    public long jobPermission() {
        return jobPermission;
    }

    /**
     * @return 비밀번호를 바꿔야 하면 {@code true}
     */
    public boolean passwordChangeRequired() {
        return passwordChangeRequired;
    }

    /**
     * @return 관리자면 {@code true}
     */
    public boolean admin() {
        return Permissions.isAdmin(deptPermission, jobPermission);
    }

    /**
     * 헤더에 보일 표기.
     *
     * <p>레거시 {@code indexHeader.jsp} 는 사번과 이름을 이어 찍었다.
     *
     * @return "사번 이름"
     */
    public String display() {
        return id + " " + name;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (passwordChangeRequired) {
            // 비밀번호를 바꾸기 전에는 관리자라도 관리자 화면을 볼 수 없다.
            return List.of(new SimpleGrantedAuthority(ROLE_PASSWORD_CHANGE));
        }
        List<GrantedAuthority> granted = new ArrayList<>();
        granted.add(new SimpleGrantedAuthority(ROLE_USER));
        if (admin()) {
            granted.add(new SimpleGrantedAuthority(ROLE_ADMIN));
        }
        return granted;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return id;
    }
}
