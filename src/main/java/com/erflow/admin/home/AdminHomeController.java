package com.erflow.admin.home;

import com.erflow.common.WorkStatus;
import java.util.List;
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
     * <p>레거시는 {@code Map<코드, 인원>} 을 그대로 내보냈고 라벨은 스크립트가 자기
     * 배열에서 꺼내 썼다. 그 배열이 서버와 어긋나 있었다(D-125). 이제 라벨을 여기서
     * 붙여 보낸다 — {@link WorkStatus} 가 유일한 정답이다.
     *
     * @return 상태별 인원. 그날 아무도 없던 상태는 들어 있지 않다
     */
    @GetMapping("/admin/graph/view")
    @ResponseBody
    public List<WorkStatusCount> graph() {
        return homeService.workCounts().entrySet().stream()
                .map(entry -> new WorkStatusCount(
                        entry.getKey(), WorkStatus.label(entry.getKey()), entry.getValue()))
                .toList();
    }
}
