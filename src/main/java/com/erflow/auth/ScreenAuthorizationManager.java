package com.erflow.auth;

import java.util.List;
import java.util.function.Supplier;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 요청 경로로 화면 권한을 판정한다.
 *
 * <p>레거시는 화면마다 상단에 같은 코드를 되풀이했다.
 *
 * <pre>
 * final String PROGRAM_CODE = "8A4364846CD2FC49...";
 * if (!WebHelper.isLogin(session) || !permissionCon.hasProgramPermission(session, PROGRAM_CODE)) {
 *     response.sendRedirect("../permissionError.jsp");
 *     return;
 * }
 * </pre>
 *
 * <p>39개 JSP 에 이 조각이 박혀 있었다. 이제 {@code screen} 테이블이 경로와 프로그램을
 * 잇고 판정은 여기 한 곳에서 한다. 새 화면을 만들며 검사를 빠뜨리는 실수도 구조적으로
 * 막힌다 — {@code screen} 에 행이 있으면 반드시 검사되고, 없으면 애초에 권한 대상이
 * 아니다.
 */
@Component
public class ScreenAuthorizationManager
        implements AuthorizationManager<RequestAuthorizationContext> {

    private final AuthMapper authMapper;

    /**
     * @param authMapper 인증 조회 매퍼
     */
    public ScreenAuthorizationManager(AuthMapper authMapper) {
        this.authMapper = authMapper;
    }

    /**
     * 접근 가부를 판정한다.
     *
     * @param authentication 인증 정보 공급자
     * @param context 요청 문맥
     * @return 허용 여부
     */
    @Override
    @Transactional(readOnly = true)
    public AuthorizationDecision authorize(
            Supplier<? extends Authentication> authentication,
            RequestAuthorizationContext context) {

        Authentication auth = authentication.get();
        if (auth == null || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof ErflowUserDetails user)) {
            return new AuthorizationDecision(false);
        }
        if (user.passwordChangeRequired()) {
            // 비밀번호를 바꾸기 전에는 아무 화면도 볼 수 없다. 레거시도 로그인시키지 않았다.
            return new AuthorizationDecision(false);
        }

        List<ScreenAccess> rules =
                authMapper.findScreenAccess(context.getRequest().getRequestURI());
        if (rules.isEmpty()) {
            // screen 에 없는 경로는 권한 대상이 아니다. 로그인만 되어 있으면 통과한다.
            // 레거시에서 PROGRAM_CODE 가 없던 화면들이 여기 해당한다.
            return new AuthorizationDecision(true);
        }

        // 같은 경로가 파라미터로 권한이 갈리는 경우가 있다. 협력업체 관리는
        // flag=1 이면 구매, flag=0 이면 영업 권한을 요구한다.
        ScreenAccess access = null;
        for (ScreenAccess rule : rules) {
            String actual = rule.paramName() == null
                    ? null
                    : context.getRequest().getParameter(rule.paramName());
            if (rule.matches(actual)) {
                access = rule;
                break;
            }
        }
        if (access == null) {
            // 갈림 규칙이 있는데 어느 것에도 걸리지 않았다. 레거시는 이런 요청에
            // 빈 programId 로 권한 검사를 돌려 통과시키지 못했다. 막는 쪽이 맞다.
            return new AuthorizationDecision(false);
        }
        return new AuthorizationDecision(Permissions.hasProgramPermission(
                user.deptPermission(), user.jobPermission(),
                access.deptLevel(), access.jobLevel()));
    }
}
