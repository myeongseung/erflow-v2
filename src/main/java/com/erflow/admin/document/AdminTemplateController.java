package com.erflow.admin.document;

import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 문서 양식 관리 화면.
 *
 * <pre>
 * documentFormList.jsp        GET  /admin/document/form-list
 * documentFormRegister.jsp    GET  /admin/document/form-register?flag=&amp;id=
 * documentFormRegisterProc    POST /admin/document/form-register-proc
 * documentFormDeleteProc      POST /admin/document/form-delete-proc
 * </pre>
 *
 * <p>추가와 수정이 <b>같은 화면</b>이다. {@code flag} 가 {@code insert} 인지
 * {@code update} 인지로 갈린다 — 화면 글자는 둘 다 «문서 양식 추가» 다.
 *
 * <p>레거시 처리는 로그인만 확인하고 관리자 여부는 그 안쪽(서비스)에서 봤다. 여기서는
 * {@code /admin/**} 이 통째로 막는다(D-053).
 */
@Controller
@RequestMapping("/admin/document")
public class AdminTemplateController {

    private static final String LIST = "/admin/document/form-list";

    private static final String RESULT = "admin/document/result";

    private final AdminTemplateService templateService;

    /**
     * @param templateService 문서 양식 업무
     */
    public AdminTemplateController(AdminTemplateService templateService) {
        this.templateService = templateService;
    }

    /**
     * 문서 양식 리스트.
     *
     * @param model 뷰 모델
     * @return 목록 템플릿
     */
    @GetMapping("/form-list")
    public String formList(Model model) {
        model.addAttribute("templates", templateService.list());
        return "admin/document/form-list";
    }

    /**
     * 문서 양식 작성 화면. 추가와 수정을 겸한다.
     *
     * @param flag {@code update} 면 기존 양식을 채워 준다. 없으면 {@code insert}
     * @param id 고칠 문서 번호. {@code update} 일 때만 본다
     * @param model 뷰 모델
     * @return 작성 템플릿. 고칠 대상이 없으면 잘못된 접근 화면
     */
    @GetMapping("/form-register")
    public String formRegister(
            @RequestParam(required = false) String flag,
            @RequestParam(required = false) Integer id,
            Model model) {

        String mode = flag == null ? "insert" : flag;
        TemplateRow template = null;
        if ("update".equals(mode)) {
            template = id == null ? null : templateService.find(id);
            if (template == null) {
                return "redirect:/access-error";
            }
        }
        model.addAttribute("flag", mode);
        // 추가일 때 레거시는 숨은 id 에 -1 을 넣는다. 처리도 그 값을 그대로 받는다.
        model.addAttribute("templateId", template == null ? -1 : template.id());
        model.addAttribute("subject", template == null ? "" : template.subject());
        model.addAttribute("content", template == null ? "" : template.content());
        return "admin/document/form-register";
    }

    /**
     * 문서 양식 등록·수정 처리.
     *
     * <p>세 값(구분·제목·내용)이 모두 있고 비어 있지 않아야 한다. 하나라도 비면 잘못된
     * 접근이다 — 레거시가 그렇게 갈랐다.
     *
     * @param flag {@code insert} 또는 {@code update}
     * @param id 고칠 문서 번호
     * @param subject 양식명
     * @param content 양식 내용(HTML)
     * @param model 뷰 모델
     * @return 결과 템플릿
     */
    @PostMapping("/form-register-proc")
    public String formRegisterProc(
            @RequestParam(required = false) String flag,
            @RequestParam(required = false) Integer id,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String content,
            Model model) {

        if (isBlank(flag) || isBlank(subject) || isBlank(content)) {
            return "redirect:/access-error";
        }
        if ("insert".equals(flag)) {
            return result(model, templateService.create(subject, content)
                    ? "문서 양식을 등록했습니다." : "문서 양식을 등록하지 못했습니다.");
        }
        if ("update".equals(flag)) {
            boolean updated = id != null && templateService.update(id, subject, content);
            return result(model, updated
                    ? "문서 양식을 수정했습니다." : "문서 양식을 수정하지 못했습니다.");
        }
        // 레거시는 그 밖의 flag 를 아무 갈래에도 넣지 않고 «등록하지 못했습니다» 를 띄웠다.
        return result(model, "문서 양식을 등록하지 못했습니다.");
    }

    /**
     * 선택한 문서 양식 삭제.
     *
     * @param templateId 지울 문서 번호들
     * @param model 뷰 모델
     * @return 결과 템플릿
     */
    @PostMapping("/form-delete-proc")
    public String formDelete(
            @RequestParam(required = false) List<Integer> templateId, Model model) {
        if (templateId == null) {
            return result(model, "삭제할 양식을 선택하십시오.");
        }
        return result(model, templateService.delete(templateId)
                ? "문서 양식을 삭제하였습니다." : "문서 양식을 삭제하지 못했습니다.");
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String result(Model model, String message) {
        model.addAttribute("message", message);
        model.addAttribute("nextPage", LIST);
        return RESULT;
    }
}
