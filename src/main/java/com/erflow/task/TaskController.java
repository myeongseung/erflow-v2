package com.erflow.task;

import com.erflow.user.UserFinder;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 수·발주 관리 화면.
 *
 * <pre>
 * purchaseTask.jsp     GET  /task/purchase-task   (발주, type=1)
 * sellTask.jsp         GET  /task/sell-task        (수주, type=0)
 * taskRegister.jsp     GET  /task/register?flag=
 * taskRegisterProc.jsp POST /task/register
 * taskUpdate.jsp       GET  /task/update?flag=&amp;id=
 * taskUpdateProc.jsp   POST /task/update
 * taskDeleteProc.jsp   POST /task/delete
 * createModal.jsp      GET  /task/history-modal?taskId=
 * </pre>
 *
 * <p>발주와 수주는 거의 같은 화면인데 {@code type} 과 권한이 갈린다. 협력업체 관리의
 * 구매/영업과 같은 구조지만, 레거시가 파일과 경로를 아예 둘로 나눠 두어({@code screen}
 * 테이블도 {@code /task/purchase-task}·{@code /task/sell-task} 로 등록) 화면 글자
 * («발주»/«수주», «의뢰 시각»/«수주 시각»)까지 달라서 템플릿도 둘로 둔다.
 *
 * <p>등록·수정·삭제·내역 모달은 다음 단계에서 붙인다.
 */
@Controller
@RequestMapping("/task")
public class TaskController {

    private final TaskService taskService;
    private final UserFinder users;

    /**
     * @param taskService 수·발주 업무
     * @param users 담당 직원명 조회
     */
    public TaskController(TaskService taskService, UserFinder users) {
        this.taskService = taskService;
        this.users = users;
    }

    /**
     * 발주 관리 목록.
     *
     * @param keyfield 검색 대상
     * @param keyword 검색어
     * @param nowPage 현재 페이지
     * @param model 뷰 모델
     * @return 발주 목록 템플릿
     */
    @GetMapping("/purchase-task")
    public String purchase(
            @RequestParam(required = false) String keyfield,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int nowPage,
            Model model) {
        return listView(TaskService.PURCHASE, "task/purchase-task", keyfield, keyword, nowPage, model);
    }

    /**
     * 수주 관리 목록.
     *
     * @param keyfield 검색 대상
     * @param keyword 검색어
     * @param nowPage 현재 페이지
     * @param model 뷰 모델
     * @return 수주 목록 템플릿
     */
    @GetMapping("/sell-task")
    public String sell(
            @RequestParam(required = false) String keyfield,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int nowPage,
            Model model) {
        return listView(TaskService.SELL, "task/sell-task", keyfield, keyword, nowPage, model);
    }

    private String listView(
            int type, String view, String keyfield, String keyword, int nowPage, Model model) {
        TaskSearch search = new TaskSearch(
                keyfield == null ? "" : keyfield, keyword == null ? "" : keyword);
        TaskService.TaskPage page = taskService.list(type, search, nowPage);

        model.addAttribute("tasks", page.rows());
        model.addAttribute("page", page.pagination());
        model.addAttribute("keyfield", keyfield == null ? "" : keyfield);
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        return view;
    }

    /**
     * 수·발주 등록 화면. 화면 글자와 권한이 {@code flag} 로 갈린다.
     *
     * <p>사실상 {@code flag} 는 언제나 sell/purchase 다. {@code screen} 테이블이
     * {@code /task/register} 를 flag 값별로 등록해 두어, 그 밖의 flag(없거나 다른 값)는
     * {@link com.erflow.auth.ScreenAuthorizationManager} 가 컨트롤러 전에 막는다. 아래
     * 분기는 그 방어가 뚫렸을 때를 위한 것이다 — 레거시 {@code taskRegister.jsp} 도 flag
     * 가 없으면 잘못된 접근으로 보냈다.
     *
     * @param flag sell 또는 purchase
     * @param model 뷰 모델
     * @return 등록 템플릿. flag 가 없으면 잘못된 접근 화면
     */
    @GetMapping("/register")
    public String registerForm(@RequestParam(required = false) String flag, Model model) {
        if (!"sell".equals(flag) && !"purchase".equals(flag)) {
            return "redirect:/access-error";
        }
        model.addAttribute("flag", flag);
        model.addAttribute("isSell", "sell".equals(flag));
        return "task/register";
    }

    /**
     * 수·발주 등록 처리.
     *
     * <p>레거시 {@code taskRegisterProc.jsp} 를 옮겼다. 다섯 값(사번·업체·문서·의뢰
     * 시각·상태)이 모두 있고 제품이 하나라도 있어야 등록한다. 하나라도 없으면 «실패».
     *
     * @param flag sell 또는 purchase
     * @param userId 담당 직원 사번
     * @param companyId 협력업체 번호
     * @param documentId 문서 번호
     * @param taskAt 의뢰 시각
     * @param status 상태 코드
     * @param productId 제품 코드 목록
     * @param count 제품별 수량 목록
     * @param model 뷰 모델
     * @return 결과 템플릿
     */
    @PostMapping("/register")
    public String register(
            @RequestParam(required = false) String flag,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) Integer companyId,
            @RequestParam(required = false) Integer documentId,
            @RequestParam(required = false) String taskAt,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) List<String> productId,
            @RequestParam(required = false) List<Integer> count,
            Model model) {

        boolean present = userId != null && companyId != null && documentId != null
                && taskAt != null && status != null && productId != null;
        boolean created = present && taskService.create(
                new Task(userId, companyId, documentId, "sell".equals(flag) ? 0 : 1,
                        taskAt, status),
                histories(productId, count));

        model.addAttribute("message", created ? "등록에 성공했습니다." : "등록에 실패했습니다.");
        model.addAttribute("nextPage", listPath(flag));
        return "task/result";
    }

    /**
     * 수·발주 수정 화면.
     *
     * <p>기존 값을 불러 채운다. 다만 의뢰 시각은 비운 채 다시 받고, 상태는 미리 고르지
     * 않는다 — 레거시 {@code taskUpdate.jsp} 그대로다.
     *
     * @param flag sell 또는 purchase
     * @param id 의뢰 번호
     * @param model 뷰 모델
     * @return 수정 템플릿. 대상이 없으면 잘못된 접근 화면
     */
    @GetMapping("/update")
    public String updateForm(
            @RequestParam(required = false) String flag,
            @RequestParam(required = false) Integer id,
            Model model) {
        TaskDetail task = id == null ? null : taskService.get(id);
        if (task == null) {
            return "redirect:/access-error";
        }
        model.addAttribute("flag", flag);
        model.addAttribute("isSell", "sell".equals(flag));
        model.addAttribute("task", task);
        model.addAttribute("userId", task.userId());
        model.addAttribute("userName", nullToEmpty(users.name(task.userId())));
        model.addAttribute("histories", taskService.histories(task.id()));
        return "task/update";
    }

    /**
     * 수·발주 수정 처리.
     *
     * <p>레거시 {@code taskUpdateProc.jsp} 를 옮겼다. 다섯 값(의뢰번호·사번·문서·의뢰
     * 시각·상태)이 모두 있고 제품이 있어야 처리한다. 협력업체는 수정 대상이 아니다.
     * type 은 언제나 0 이라 발주 수정은 실패한다(D-044) — 그래도 이력은 갈린다.
     *
     * @param flag sell 또는 purchase
     * @param taskId 의뢰 번호
     * @param userId 담당 직원 사번
     * @param documentId 문서 번호
     * @param taskAt 의뢰 시각
     * @param status 상태 코드
     * @param productId 제품 코드 목록
     * @param count 제품별 수량 목록
     * @param model 뷰 모델
     * @return 결과 템플릿
     */
    @PostMapping("/update")
    public String update(
            @RequestParam(required = false) String flag,
            @RequestParam(required = false) Integer taskId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) Integer documentId,
            @RequestParam(required = false) String taskAt,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) List<String> productId,
            @RequestParam(required = false) List<Integer> count,
            Model model) {

        boolean present = taskId != null && userId != null && documentId != null
                && taskAt != null && status != null && productId != null;
        boolean updated = present && taskService.update(
                // 레거시는 type 을 넣지 않아 언제나 0 이었고 발주 수정이 늘
                // 실패했다(D-044). 2단계에서 flag 로 채운다(D-106).
                new TaskUpdate(taskId, blankToNull(userId), documentId,
                        blankToNull(taskAt), status,
                        "sell".equals(flag) ? TaskService.SELL : TaskService.PURCHASE),
                histories(productId, count));

        model.addAttribute("message", updated ? "수정에 성공했습니다." : "수정에 실패했습니다.");
        model.addAttribute("nextPage", listPath(flag));
        return "task/result";
    }

    /**
     * 선택된 수·발주 삭제.
     *
     * @param flag sell 또는 purchase
     * @param taskId 지울 의뢰 번호 목록
     * @param model 뷰 모델
     * @return 결과 템플릿
     */
    @PostMapping("/delete")
    public String delete(
            @RequestParam(required = false) String flag,
            @RequestParam(required = false) List<Integer> taskId,
            Model model) {
        int type = "sell".equals(flag) ? TaskService.SELL : TaskService.PURCHASE;
        boolean deleted = taskId != null && taskService.delete(taskId, type);
        model.addAttribute("message", deleted ? "삭제에 성공했습니다." : "삭제에 실패했습니다.");
        model.addAttribute("nextPage", listPath(flag));
        return "task/result";
    }

    /**
     * 내역 모달. 한 수·발주의 제품·수량을 보여준다.
     *
     * @param taskId 수·발주 번호
     * @param model 뷰 모델
     * @return 모달 템플릿. 번호가 없으면 잘못된 접근 화면
     */
    @GetMapping("/history-modal")
    public String historyModal(
            @RequestParam(required = false) Integer taskId, Model model) {
        if (taskId == null || taskId == 0) {
            // 레거시 createModal.jsp 가 taskId 가 0 이면 accessError 로 보냈다.
            return "redirect:/access-error";
        }
        model.addAttribute("histories", taskService.histories(taskId));
        return "task/history-modal";
    }

    /**
     * 제품 코드·수량 배열을 이력 목록으로 짝짓는다.
     *
     * @param productIds 제품 코드 목록
     * @param counts 수량 목록. 짧으면 0 으로 본다
     * @return 이력 목록. taskId 는 등록 시 채운다
     */
    private static List<TaskHistory> histories(List<String> productIds, List<Integer> counts) {
        List<TaskHistory> histories = new ArrayList<>();
        for (int i = 0; i < productIds.size(); i++) {
            int quantity = counts != null && i < counts.size() ? counts.get(i) : 0;
            histories.add(new TaskHistory(0, productIds.get(i), quantity));
        }
        return histories;
    }

    private static String listPath(String flag) {
        // 레거시: flag + "Task.jsp" -> 목록으로 돌아간다.
        return "sell".equals(flag) ? "/task/sell-task" : "/task/purchase-task";
    }

    private static String blankToNull(String value) {
        // 레거시 updateProc 가 빈 문자열을 null 로 바꿔 넣었다.
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
