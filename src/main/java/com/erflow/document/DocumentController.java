package com.erflow.document;

import com.erflow.auth.ErflowUserDetails;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 문서 관리 화면.
 *
 * <pre>
 * documentList.jsp        GET  /document/list
 * documentRegister.jsp    GET  /document/register?flag=&amp;docId=&amp;template=
 * documentRegisterProc    POST /document/register-proc
 * documentDeleteProc      POST /document/delete-proc
 * </pre>
 *
 * <p>목록은 <b>내 문서만</b> 보여준다. 작성 화면은 추가와 수정을 겸하고, 양식을 고르면
 * 그 양식의 내용을 편집기에 부어 넣기 위해 화면을 다시 그린다.
 *
 * <p>목록과 작성·삭제의 <b>권한 프로그램이 다르다</b>. {@code screen} 표가 그렇게 걸어
 * 두었으므로 여기서는 아무것도 하지 않는다.
 */
@Controller
@RequestMapping("/document")
public class DocumentController {

    private final DocumentService documentService;

    /**
     * @param documentService 문서 업무
     */
    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * 문서 리스트.
     *
     * @param keyfield 검색할 칸
     * @param keyword 검색어
     * @param nowPage 현재 페이지
     * @param user 로그인 사용자
     * @param model 뷰 모델
     * @return 목록 템플릿
     */
    @GetMapping("/list")
    public String list(
            @RequestParam(required = false) String keyfield,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int nowPage,
            @AuthenticationPrincipal ErflowUserDetails user,
            Model model) {

        DocumentSearch search = new DocumentSearch(
                keyfield == null ? "" : keyfield, keyword == null ? "" : keyword);
        DocumentService.DocumentPage page = documentService.list(search, user.id(), nowPage);

        model.addAttribute("documents", page.rows());
        model.addAttribute("page", page.pagination());
        model.addAttribute("keyfield", search.keyfield());
        model.addAttribute("keyword", search.keyword());
        return "document/list";
    }

    /**
     * 문서 작성 화면. 추가와 수정을 겸한다.
     *
     * <p>양식을 고르면 화면이 {@code template} 을 달고 다시 열린다. 레거시는 그때
     * 편집기 내용을 무조건 그 양식으로 덮어 쓰던 글이 사라졌다(D-076). 2단계에서
     * «내용이 비어 있을 때만 덮는다» 로 고쳤다(D-110). 타이핑만 하고 저장하지 않은
     * 글은 서버에 온 적이 없어 여기서 지킬 수 없다 — 지키는 것은 저장된 내용이다.
     *
     * <p>남의 문서를 고치려 하면 잘못된 접근으로 보낸다.
     *
     * @param flag {@code update} 면 기존 문서를 채운다. 없으면 {@code insert}
     * @param docId 문서번호
     * @param template 고른 양식 번호
     * @param user 로그인 사용자
     * @param model 뷰 모델
     * @return 작성 템플릿
     */
    @GetMapping("/register")
    public String registerForm(
            @RequestParam(required = false) String flag,
            @RequestParam(required = false) Long docId,
            @RequestParam(required = false) Integer template,
            @AuthenticationPrincipal ErflowUserDetails user,
            Model model) {

        String mode = flag == null ? "insert" : flag;
        DocumentDetail document = docId == null ? null : documentService.get(docId);
        if ("update".equals(mode)
                && (document == null || !user.id().equals(document.userId()))) {
            return "redirect:/access-error";
        }
        String content = document == null ? "" : document.content();
        if (template != null && isBlank(content)) {
            // 내용이 비어 있을 때만 양식으로 채운다(D-110). 레거시는 무조건 덮었다.
            content = documentService.templateContent(template);
        }
        model.addAttribute("flag", mode);
        // 레거시는 새 문서일 때 숨은 id 에 -1 을 넣는다.
        model.addAttribute("docId", document == null ? -1L : document.id());
        model.addAttribute("subject", document == null ? "" : document.subject());
        model.addAttribute("content", content);
        model.addAttribute("templates", documentService.templates());
        model.addAttribute("selectedTemplate", template);
        return "document/register";
    }

    /**
     * 문서 등록·수정 처리.
     *
     * <p>문서번호·본문·구분이 모두 있어야 한다. 하나라도 비면 레거시는 <b>권한 오류</b>
     * 화면으로 보냈다 — 잘못된 접근이 아니라 권한 화면이다. 그대로 옮긴다.
     *
     * @param id 문서번호
     * @param flag {@code insert} 또는 {@code update}
     * @param subject 제목
     * @param content 본문(HTML)
     * @param templateId 양식 번호
     * @param user 로그인 사용자
     * @param model 뷰 모델
     * @return 결과 템플릿
     */
    @PostMapping("/register-proc")
    public String register(
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String flag,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String content,
            @RequestParam(defaultValue = "0") int templateId,
            @AuthenticationPrincipal ErflowUserDetails user,
            Model model) {

        if (isBlank(id) || isBlank(content) || isBlank(flag)) {
            return "redirect:/permission-error";
        }
        long docId = number(id);

        if ("insert".equals(flag)) {
            boolean created = documentService.create(user.id(), templateId, subject, content);
            return result(model,
                    created ? "문서를 등록했습니다." : "문서를 등록하지 못했습니다.",
                    created ? "/document/list" : "/document/register");
        }
        if ("update".equals(flag)) {
            DocumentService.UpdateResult done =
                    documentService.update(docId, user.id(), templateId, subject, content);
            return switch (done) {
                case UPDATED -> result(model, "문서를 수정했습니다.", "/document/list");
                case RENEWED -> result(model,
                        "이미 결재 진행된 문서이므로, 문서를 새로 등록합니다.", "/document/list");
                case FAILED -> result(model, "문서를 수정하지 못했습니다.",
                        "/document/register?flag=update&id=" + docId);
            };
        }
        // 레거시는 그 밖의 flag 를 아무 갈래에도 넣지 않고 «등록하지 못했습니다» 를 띄웠다.
        return result(model, "문서를 등록하지 못했습니다.", "/document/register");
    }

    /**
     * 선택한 문서 삭제.
     *
     * @param docId 지울 문서번호들
     * @param model 뷰 모델
     * @return 결과 템플릿
     */
    @PostMapping("/delete-proc")
    public String delete(@RequestParam(required = false) List<Long> docId, Model model) {
        if (docId == null) {
            return result(model, "선택한 문서가 없습니다.", "/document/list");
        }
        return result(model, documentService.delete(docId)
                        ? "문서를 삭제하였습니다."
                        : "결재 진행 중인 항목 외의 문서를 삭제하였습니다.",
                "/document/list");
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * 문서번호를 숫자로 읽는다.
     *
     * <p>레거시는 읽지 못해도 그냥 넘어가 {@code -1} 로 처리했다(예외를 삼킨다).
     */
    private static long number(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return -1L;
        }
    }

    private String result(Model model, String message, String nextPage) {
        model.addAttribute("message", message);
        model.addAttribute("nextPage", nextPage);
        return "document/result";
    }
}
