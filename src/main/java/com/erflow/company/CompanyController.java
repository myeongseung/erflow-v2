package com.erflow.company;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 협력업체 관리 화면.
 *
 * <pre>
 * companyList.jsp          GET  /company/list?flag=
 * companyRegister.jsp      GET  /company/register?flag=
 * companyRegisterProc.jsp  POST /company/register
 * companyUpdate.jsp        GET  /company/update?flag=&amp;id=
 * companyUpdateProc.jsp    POST /company/update
 * companyDeleteProc.jsp    POST /company/delete
 * </pre>
 *
 * <p>구매(1)와 영업(0)이 같은 화면을 쓰고 {@code flag} 로 갈린다. 권한도 그렇게
 * 갈리므로({@code migration/design/00-decisions.md} D-016) 모든 요청이 {@code flag}
 * 를 달고 다녀야 한다. 등록·수정 처리도 마찬가지다 — 레거시 {@code *Proc.jsp} 는
 * 로그인만 검사했으나, 화면과 같은 URL 로 합치면서 같은 권한을 요구하게 된다(D-018).
 */
@Controller
@RequestMapping("/company")
public class CompanyController {

    private static final String RESULT = "company/result";

    private final CompanyService companyService;

    /**
     * @param companyService 협력업체 업무
     */
    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    /**
     * 협력업체 목록.
     *
     * @param flag 0 영업 / 1 구매
     * @param keyfield 검색 대상
     * @param keyword 검색어
     * @param nowPage 현재 페이지
     * @param model 뷰 모델
     * @return 목록 템플릿
     */
    @GetMapping("/list")
    public String list(
            @RequestParam int flag,
            @RequestParam(required = false) String keyfield,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int nowPage,
            Model model) {

        CompanySearch search = companyService.toSearch(keyfield, keyword);
        CompanyService.CompanyPage page = companyService.list(flag, search, nowPage);

        // 목록은 업종코드가 아니라 업종명을 보여준다. 레거시도 화면에서 풀어 찍었다.
        Map<String, String> fieldNames = new LinkedHashMap<>();
        for (Company company : page.rows()) {
            fieldNames.put(company.field(), companyService.fieldName(company.field()));
        }

        model.addAttribute("companies", page.rows());
        model.addAttribute("fieldNames", fieldNames);
        model.addAttribute("page", page.pagination());
        model.addAttribute("flag", flag);
        model.addAttribute("keyfield", keyfield == null ? "" : keyfield);
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        return "company/list";
    }

    /**
     * 협력업체 등록 화면.
     *
     * @param flag 0 영업 / 1 구매
     * @param model 뷰 모델
     * @return 등록 템플릿
     */
    @GetMapping("/register")
    public String registerForm(@RequestParam int flag, Model model) {
        model.addAttribute("flag", flag);
        return "company/register";
    }

    /**
     * 협력업체 등록 처리.
     *
     * <p>레거시는 열 개 파라미터 중 하나라도 없으면 등록하지 않았고, 빈 문자열은
     * {@code null} 로 바꿔 넣었다.
     *
     * @param form 화면에서 넘어온 값
     * @param model 뷰 모델
     * @return 결과 템플릿
     */
    @PostMapping("/register")
    public String register(CompanyForm form, Model model) {
        boolean created = form.complete() && companyService.create(form.toCompany(0));
        return result(model, created ? "등록에 성공했습니다." : "등록에 실패했습니다.", form.status());
    }

    /**
     * 협력업체 수정 화면.
     *
     * @param flag 0 영업 / 1 구매
     * @param id 번호
     * @param model 뷰 모델
     * @return 수정 템플릿. 대상이 없으면 오류 화면으로 보낸다
     */
    @GetMapping("/update")
    public String updateForm(
            @RequestParam int flag, @RequestParam int id, Model model) {

        Company company = companyService.get(id);
        if (company == null) {
            return "redirect:/access-error";
        }
        model.addAttribute("company", company);
        model.addAttribute("flag", flag);
        model.addAttribute("fieldName", nullToEmpty(companyService.fieldName(company.field())));
        model.addAttribute("bankName", nullToEmpty(companyService.bankName(company.bankCode())));
        return "company/update";
    }

    /**
     * 협력업체 수정 처리.
     *
     * @param form 화면에서 넘어온 값
     * @param model 뷰 모델
     * @return 결과 템플릿
     */
    @PostMapping("/update")
    public String update(CompanyForm form, Model model) {
        boolean updated = form.id() != null && form.complete()
                && companyService.update(form.toCompany(form.id()));
        // 레거시 companyUpdateProc.jsp 는 성공 문구가 "등록에 성공했습니다." 였다.
        // 수정 화면인데 등록이라고 알린다. 문구를 고치는 것은 이관이 아니므로 그대로 둔다.
        return result(model, updated ? "등록에 성공했습니다." : "등록에 실패했습니다.", form.status());
    }

    /**
     * 선택된 협력업체 삭제.
     *
     * @param flag 0 영업 / 1 구매
     * @param companyId 선택된 번호 목록
     * @param model 뷰 모델
     * @return 결과 템플릿
     */
    @PostMapping("/delete")
    public String delete(
            @RequestParam(required = false) String flag,
            @RequestParam(required = false) List<Integer> companyId,
            Model model) {

        boolean valid = companyId != null && flag != null && !flag.isBlank();
        boolean deleted = valid && companyService.delete(companyId);
        return result(model, deleted ? "삭제에 성공했습니다." : "삭제에 실패했습니다.", flag);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String result(Model model, String message, String flag) {
        model.addAttribute("message", message);
        model.addAttribute("nextPage", "/company/list?flag=" + (flag == null ? "" : flag));
        return RESULT;
    }
}
