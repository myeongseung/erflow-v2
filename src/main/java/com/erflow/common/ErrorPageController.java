package com.erflow.common;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 레거시 안내 화면.
 *
 * <p>레거시는 화면마다 {@code response.sendRedirect("../permissionError.jsp")} 처럼
 * 안내 화면으로 보냈다. 도메인마다 필요하므로 공통으로 둔다.
 *
 * <p>이관 전 화면을 대신하던 발판은 더 없다. 마지막 둘이 프로필과 메인이었다.
 */
@Controller
public class ErrorPageController {

    /**
     * 권한 없음 안내.
     *
     * @return 안내 템플릿
     */
    @GetMapping("/permission-error")
    public String permissionError() {
        return "error/permission-error";
    }

    /**
     * 잘못된 접근 안내.
     *
     * @return 안내 템플릿
     */
    @GetMapping("/access-error")
    public String accessError() {
        return "error/access-error";
    }

    /**
     * 없는 페이지 안내.
     *
     * <p>실제 404 응답에도 이 화면이 뜬다({@link ErrorViewConfig}). 매핑을 따로 두는
     * 것은 레거시에서 {@code notFoundError.jsp} 가 직접 열리는 주소였기 때문이다.
     *
     * @return 안내 템플릿
     */
    @GetMapping("/not-found-error")
    public String notFoundError() {
        return "error/not-found-error";
    }

    /**
     * 내부 서버 오류 안내.
     *
     * @return 안내 템플릿
     */
    @GetMapping("/internal-server-error")
    public String internalServerError() {
        return "error/internal-server-error";
    }
}
