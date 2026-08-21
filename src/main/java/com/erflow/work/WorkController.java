package com.erflow.work;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 근태 확인 화면.
 *
 * <pre>
 * work/work.jsp   GET /work?date=&amp;keyfield=&amp;keyword=&amp;nowPage=
 * </pre>
 *
 * <p>전 직원의 한 달 근태를 8명씩 끊어 보여준다. 권한은 {@code screen} 표가 건다
 * («근태 관리» 프로그램) — 여기서는 아무것도 하지 않는다.
 */
@Controller
public class WorkController {

    private final WorkService workService;

    /**
     * @param workService 근태 업무
     */
    public WorkController(WorkService workService) {
        this.workService = workService;
    }

    /**
     * 근태 확인.
     *
     * <p>읽을 수 없는 달이면 잘못된 접근으로 보낸다 — 레거시가 파싱에 실패하면
     * {@code accessError.jsp} 로 보냈다. 빈 값은 «이번 달» 이다.
     *
     * @param date {@code yyyy-MM}. 없으면 이번 달
     * @param keyfield 검색할 칸({@code dept} 또는 {@code name}). 그 외는 전체조회
     * @param keyword 검색어
     * @param nowPage 현재 페이지
     * @param model 뷰 모델
     * @return 근태 템플릿
     */
    @GetMapping("/work")
    public String view(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String keyfield,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int nowPage,
            Model model) {

        YearMonth month = workService.thisMonth();
        if (date != null && !date.isBlank()) {
            try {
                month = YearMonth.parse(date.trim());
            } catch (DateTimeParseException e) {
                return "redirect:/access-error";
            }
        }
        // 레거시는 keyfield 로 검색어를 갈랐다 — dept 면 부서, name 이면 이름.
        String field = keyfield == null ? "" : keyfield;
        String dept = "dept".equals(field) ? keyword : "";
        String name = "name".equals(field) ? keyword : "";

        WorkService.WorkPage page = workService.page(month, dept, name, nowPage);
        model.addAttribute("rows", page.rows());
        model.addAttribute("page", page.pagination());
        model.addAttribute("month", month.toString());
        model.addAttribute("minMonth", workService.minMonth().toString());
        model.addAttribute("firstHeaders", workService.headers(month, 1, 16));
        model.addAttribute("secondHeaders", workService.headers(month, 17, 32));
        model.addAttribute("keyfield", field);
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        return "work/work";
    }
}
