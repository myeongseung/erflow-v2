package com.erflow.admin.home;

import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 관리자 홈(대시보드).
 *
 * <pre>
 * admin/admin.jsp          GET /admin
 * GraphWorkViewServlet     GET /admin/graph/view   (JSON)
 * </pre>
 *
 * <p>그래프는 화면이 아니라 <b>스크립트가 부르는 주소</b>다. 레거시도 서블릿이었고 경로도
 * 같다 — 화면이 아니므로 라우트 변환 규칙(D-005)의 대상이 아니다.
 */
@Controller
public class AdminHomeController {

    private final AdminHomeService homeService;

    /**
     * @param homeService 대시보드 업무
     */
    public AdminHomeController(AdminHomeService homeService) {
        this.homeService = homeService;
    }

    /**
     * 관리자 홈.
     *
     * @param model 뷰 모델
     * @return 대시보드 템플릿
     */
    @GetMapping("/admin")
    public String home(Model model) {
        AdminHomeService.Dashboard dashboard = homeService.dashboard();
        model.addAttribute("proposals", dashboard.proposals());
        model.addAttribute("tasks", dashboard.tasks());
        model.addAttribute("notices", dashboard.notices());
        model.addAttribute("works", dashboard.works());
        return "admin/home";
    }

    /**
     * 근무 현황 그래프가 읽는 값.
     *
     * @return 상태 코드별 인원
     */
    @GetMapping("/admin/graph/view")
    @ResponseBody
    public Map<Integer, Integer> graph() {
        return homeService.workCounts();
    }
}
