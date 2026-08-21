package com.erflow.process;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 공정 관리 화면.
 *
 * <pre>
 * processList.jsp        GET  /process/list
 * processRegister.jsp    GET  /process/register
 * ProcessRegisterRequest POST /process/processRegister      (JSON)
 * processUpdate.jsp      GET  /process/update?id=&amp;name=  (팝업)
 * processUpdateProc.jsp  POST /process/update-proc
 * processDeleteProc.jsp  POST /process/delete-proc
 * </pre>
 *
 * <p>등록은 화면에서 표에 공정을 쌓은 뒤 <b>한 번에</b> JSON 으로 보낸다. 레거시도
 * 서블릿이었고 경로도 같다 — 화면이 아니므로 라우트 변환 규칙(D-005) 대상이 아니다.
 *
 * <p>수정은 목록에서 {@code window.open} 으로 여는 작은 창이고, 저장하면 부모를 새로
 * 고친 뒤 닫는다.
 */
@Controller
@RequestMapping("/process")
public class ProcessController {

    private final ProcessService processService;

    /**
     * @param processService 공정 업무
     */
    public ProcessController(ProcessService processService) {
        this.processService = processService;
    }

    /**
     * 공정 관리 목록.
     *
     * @param keyfield 검색할 칸
     * @param keyword 검색어
     * @param nowPage 현재 페이지
     * @param model 뷰 모델
     * @return 목록 템플릿
     */
    @GetMapping("/list")
    public String list(
            @RequestParam(required = false) String keyfield,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int nowPage,
            Model model) {

        ProcessSearch search = new ProcessSearch(
                keyfield == null ? "" : keyfield, keyword == null ? "" : keyword);
        ProcessService.ProcessPage page = processService.list(search, nowPage);

        model.addAttribute("processes", page.rows());
        model.addAttribute("page", page.pagination());
        model.addAttribute("keyfield", search.keyfield());
        model.addAttribute("keyword", search.keyword());
        return "process/list";
    }

    /**
     * 공정 등록 화면.
     *
     * @return 등록 템플릿
     */
    @GetMapping("/register")
    public String registerForm() {
        return "process/register";
    }

    /**
     * 공정 등록 처리. 화면이 쌓은 표를 통째로 받는다.
     *
     * <p>레거시는 응답에 «이벤트 추가에 실패했습니다» 라고 적는다 — 일정 등록 서블릿을
     * 복사한 흔적이다. 화면 스크립트는 {@code status} 만 보고 그 글자는 쓰지 않는다.
     * 그대로 옮긴다.
     *
     * @param steps 공정ID·공정명 짝의 목록
     * @return {@code status} 와 {@code message}
     */
    @PostMapping("/processRegister")
    @ResponseBody
    public Map<String, String> register(
            @RequestBody(required = false) List<ProcessService.ProcessStep> steps) {

        boolean done = processService.createChain(steps);
        return done
                ? Map.of("status", "success", "message", "이벤트가 성공적으로 추가되었습니다.")
                : Map.of("status", "error", "message", "이벤트 추가에 실패했습니다.");
    }

    /**
     * 공정명 변경 팝업.
     *
     * <p>레거시는 이 화면에 권한 검사가 없다. 목록에서 넘겨준 이름을 그대로 입력칸에
     * 채운다 — DB 를 다시 읽지 않는다.
     *
     * @param id 공정ID
     * @param name 공정명
     * @param model 뷰 모델
     * @return 수정 템플릿
     */
    @GetMapping("/update")
    public String updateForm(
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String name,
            Model model) {

        model.addAttribute("processId", id == null ? "" : id);
        model.addAttribute("processName", name == null ? "" : name);
        return "process/update";
    }

    /**
     * 공정명 변경 처리.
     *
     * @param id 공정ID
     * @param processName 새 이름
     * @param model 뷰 모델
     * @return 팝업 결과 템플릿
     */
    @PostMapping("/update-proc")
    public String update(
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String processName,
            Model model) {

        boolean done = id != null && processName != null
                && processService.rename(id, processName);
        model.addAttribute("message", done ? "수정에 성공했습니다." : "수정에 실패했습니다.");
        model.addAttribute("closePopup", true);
        model.addAttribute("nextPage", "/process/list");
        return "process/result";
    }

    /**
     * 선택한 공정 삭제.
     *
     * @param processId 지울 공정ID 들
     * @param model 뷰 모델
     * @return 결과 템플릿
     */
    @PostMapping("/delete-proc")
    public String delete(
            @RequestParam(required = false) List<String> processId, Model model) {

        String message;
        if (processId == null) {
            // 레거시는 «삭제에 실패했습니다» 를 넣었다가 곧바로 덮어쓴다.
            message = "선택한 내역을 삭제하지 못했습니다.";
        } else {
            message = processService.delete(processId)
                    ? "삭제에 성공했습니다." : "선택한 내역을 삭제하지 못했습니다.";
        }
        model.addAttribute("message", message);
        model.addAttribute("closePopup", false);
        model.addAttribute("nextPage", "/process/list");
        return "process/result";
    }
}
