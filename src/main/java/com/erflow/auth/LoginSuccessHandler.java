package com.erflow.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

/**
 * 로그인 성공 후 보낼 곳을 정한다.
 *
 * <p>레거시 {@code LoginServlet} 의 분기를 그대로 옮긴다.
 *
 * <pre>
 * 비밀번호 == 사번  ->  changePassword.jsp   (최초 로그인. 세션에 tempId 만 담았다)
 * 관리자            ->  ../admin/admin.jsp
 * 그 외             ->  ../index.jsp
 * </pre>
 */
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    /**
     * 로그인 성공을 처리한다.
     *
     * @param request 요청
     * @param response 응답
     * @param authentication 인증 결과
     * @throws IOException 리다이렉트 실패 시
     */
    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException {

        String target = "/index";
        if (authentication.getPrincipal() instanceof ErflowUserDetails user) {
            if (user.passwordChangeRequired()) {
                target = "/login/change-password";
            } else if (user.admin()) {
                target = "/admin";
            }
        }
        response.sendRedirect(request.getContextPath() + target);
    }
}
