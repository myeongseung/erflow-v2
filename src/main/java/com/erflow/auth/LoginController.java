package com.erflow.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 로그인·비밀번호 변경 화면.
 *
 * <pre>
 * login.jsp              GET  /login
 * LoginServlet           POST /login              (Spring Security 가 처리)
 * changePassword.jsp     GET  /login/change-password
 * ChangePasswordServlet  POST /login/change-password
 * findPassword.jsp       GET  /login/find-password
 * passwordError.html     GET  /login/password-error
 * passwordOk.html        GET  /login/password-ok
 * logoutProc.jsp         POST /login/logout-proc  (Spring Security 가 처리)
 * </pre>
 */
@Controller
public class LoginController {

    private final AuthMapper authMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * @param authMapper 인증 조회 매퍼
     * @param passwordEncoder 비밀번호 인코더
     */
    public LoginController(AuthMapper authMapper, PasswordEncoder passwordEncoder) {
        this.authMapper = authMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 로그인 화면.
     *
     * <p>이미 로그인한 사람은 화면을 보지 못하고 되돌아간다 — 관리자는 관리자 화면으로,
     * 나머지는 메인으로. 레거시가 화면 맨 위에서 그렇게 갈랐다(D-090).
     *
     * @param user 로그인 사용자. 로그인 전이면 {@code null}
     * @return 로그인 템플릿, 또는 되돌아갈 곳
     */
    @GetMapping("/login")
    public String loginForm(@AuthenticationPrincipal ErflowUserDetails user) {
        if (user != null && !user.passwordChangeRequired()) {
            return user.admin() ? "redirect:/admin" : "redirect:/index";
        }
        return "login/login";
    }

    /**
     * 비밀번호 변경 화면.
     *
     * @return 비밀번호 변경 템플릿
     */
    @GetMapping("/login/change-password")
    public String changePasswordForm() {
        return "login/change-password";
    }

    /**
     * 비밀번호 변경 처리.
     *
     * <p>레거시 {@code ChangePasswordServlet} 은 두 입력이 같을 때만 바꾸고, 성공하면
     * 세션을 통째로 버린 뒤 안내 화면으로 보냈다. 실패하면 권한 오류 화면으로 보냈다.
     *
     * @param password 새 비밀번호
     * @param rePassword 새 비밀번호 확인
     * @param user 현재 사용자
     * @param request 세션을 끊기 위한 요청
     * @return 결과 화면으로의 이동
     */
    @PostMapping("/login/change-password")
    @Transactional
    public String changePassword(
            @RequestParam(required = false) String password,
            @RequestParam(required = false) String rePassword,
            @AuthenticationPrincipal ErflowUserDetails user,
            HttpServletRequest request) {

        boolean changed = user != null
                && password != null
                && password.equals(rePassword)
                && authMapper.updatePassword(user.id(), passwordEncoder.encode(password)) > 0;

        if (!changed) {
            return "redirect:/permission-error";
        }
        // 레거시도 변경 뒤 세션을 버려 다시 로그인하게 했다
        request.getSession().invalidate();
        return "redirect:/login/password-ok";
    }

    /**
     * 비밀번호 찾기 화면.
     *
     * <p>«send» 를 받는 메일 발송은 이관 범위 밖이다(O-011). 화면만 옮겼다 —
     * 로그인 화면의 «Forgot Password?» 가 여기로 오는데 지금까지 갈 곳이 없었다.
     *
     * @return 비밀번호 찾기 템플릿
     */
    @GetMapping("/login/find-password")
    public String findPasswordForm() {
        return "login/find-password";
    }

    /**
     * 로그인 실패 안내.
     *
     * @return 안내 템플릿
     */
    @GetMapping("/login/password-error")
    public String passwordError() {
        return "login/password-error";
    }

    /**
     * 비밀번호 변경 완료 안내.
     *
     * @return 안내 템플릿
     */
    @GetMapping("/login/password-ok")
    public String passwordOk() {
        return "login/password-ok";
    }
}
