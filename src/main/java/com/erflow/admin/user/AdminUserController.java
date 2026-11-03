package com.erflow.admin.user;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 사원 관리 화면.
 *
 * <pre>
 * admin/user/userList.jsp        GET  /admin/user/list
 * admin/user/userRegister.jsp    GET  /admin/user/register    (목록 + 추가 대화상자)
 * admin/user/userRegisterProc    POST /admin/user/register-proc
 * admin/user/userUpdate.jsp      GET  /admin/user/update?id=  (목록 + 수정 대화상자)
 * admin/user/userUpdateProc      POST /admin/user/update-proc
 * </pre>
 *
 * <p>주소 팝업(userAddress.jsp)은 없앴다 — 목록 줄마다 주소 대화상자가 붙는다(D-157).
 *
 * <p>권한은 여기서 보지 않는다. 레거시는 화면마다 {@code isAdmin(session)} 을 물었지만,
 * 이 아래는 {@code SecurityConfig} 의 {@code /admin/**} 규칙이 통째로 막는다(D-053).
 */
@Controller
@RequestMapping("/admin/user")
public class AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * @param adminUserService 사원 관리 업무
     */
    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    /**
     * 사원 리스트.
     *
     * @param keyfield 검색할 칸(사번/이름/부서/직급). 없으면 전체
     * @param keyword 검색어
     * @param nowPage 현재 페이지
     * @param model 뷰 모델
     * @return 사원 리스트 템플릿
     */
    @GetMapping("/list")
    public String list(
            @RequestParam(required = false) String keyfield,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int nowPage,
            Model model) {
        fillList(keyfield, keyword, nowPage, model);
        return "admin/user/list";
    }

    /**
     * 목록 한 쪽과 추가 대화상자가 쓰는 값을 모델에 채운다 (D-148).
     *
     * @param keyfield 검색할 칸. 없으면 전체
     * @param keyword 검색어
     * @param nowPage 현재 페이지
     * @param model 뷰 모델
     */
    private void fillList(String keyfield, String keyword, int nowPage, Model model) {
        AdminUserSearch search = new AdminUserSearch(
                keyfield == null ? "" : keyfield, keyword == null ? "" : keyword);
        AdminUserService.UserPage page = adminUserService.list(search, nowPage);

        model.addAttribute("users", page.rows());
        model.addAttribute("page", page.pagination());
        model.addAttribute("keyfield", search.keyfield());
        model.addAttribute("keyword", search.keyword());
        model.addAttribute("options", adminUserService.registerForm());
    }

    /**
     * 사원 추가 화면 — 목록을 추가 대화상자가 열린 채로 그린다 (D-148).
     *
     * <p>추가 폼은 목록 화면 위의 대화상자가 됐다. 옛 주소로 들어와도 같은 곳에
     * 닿도록 주소는 남긴다.
     *
     * @param model 뷰 모델
     * @return 사원 관리 템플릿
     */
    @GetMapping("/register")
    public String registerForm(Model model) {
        fillList(null, null, 1, model);
        model.addAttribute("openAdd", true);
        return "admin/user/list";
    }

    /**
     * 사원 추가 처리.
     *
     * <p>레거시 {@code userRegisterProc.jsp} 를 옮겼다. 여섯 값(사번·이름·주민번호·
     * 이메일·직급·부서)이 요청에 있어야 등록한다. 비어 있어도 된다 — «없는 것»과
     * «빈 것»을 가른다. 빈 값은 {@code null} 로 저장된다.
     *
     * <p>레거시는 화면이 보낸 값 셋 — 주소 두 줄과 휴대 전화 — 을 버렸다(D-057).
     * 주소는 있지도 않은 자리에서 꺼내 썼고 휴대 전화는 아예 읽지 않았다. 2단계에서
     * 셋 다 받아 저장한다(D-104).
     *
     * @param id 사번
     * @param name 이름
     * @param socialNumber 주민등록번호
     * @param email 이메일
     * @param postalCode 우편번호
     * @param address1 도로명 주소
     * @param address2 상세 주소
     * @param job 직급 번호
     * @param dept 부서 번호
     * @param extensionPhone 내선 번호
     * @param mobilePhone 휴대 전화
     * @param model 뷰 모델
     * @return 결과 템플릿
     */
    @PostMapping("/register-proc")
    public String register(
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String socialNumber,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String postalCode,
            @RequestParam(required = false) String address1,
            @RequestParam(required = false) String address2,
            @RequestParam(required = false) Integer job,
            @RequestParam(required = false) Integer dept,
            @RequestParam(required = false) String extensionPhone,
            @RequestParam(required = false) String mobilePhone,
            Model model) {

        boolean given = id != null && name != null && socialNumber != null && email != null
                && job != null && dept != null;
        boolean created = given && adminUserService.register(new AdminUserEdit(
                blankToNull(id), blankToNull(name), socialNumber, blankToNull(email),
                postalCode, blankToNull(address1), blankToNull(address2),
                job, dept, extensionPhone, blankToNull(mobilePhone)));

        return result(model, created ? "등록에 성공했습니다." : "등록에 실패했습니다.",
                created ? "/admin/user/list" : "/admin/user/register");
    }

    /**
     * 사원 수정 — 목록을 그 사원의 수정 대화상자가 열린 채로 그린다 (D-157).
     *
     * <p>사번이 없거나 그런 사원이 없으면 잘못된 접근으로 보낸다.
     *
     * @param id 사번
     * @param model 뷰 모델
     * @return 사원 관리 템플릿. 대상이 없으면 잘못된 접근 화면
     */
    @GetMapping("/update")
    public String updateForm(@RequestParam(required = false) String id, Model model) {
        AdminUserService.UserForm form = id == null ? null : adminUserService.updateForm(id);
        if (form == null) {
            return "redirect:/access-error";
        }
        fillList(null, null, 1, model);
        model.addAttribute("editUser", form);
        return "admin/user/list";
    }

    /**
     * 사원 수정 처리.
     *
     * <p>레거시 {@code userUpdateProc.jsp} 를 옮겼다. 아홉 값이 요청에 있어야 고친다.
     *
     * <p>레거시는 휴대 전화를 읽지 않으면서 갱신 문장에는 그 칸을 두어, 수정할 때마다
     * 저장된 번호가 지워졌다(D-057). 2단계에서 받아 저장한다(D-104).
     * 주민등록번호는 갱신 문장에 없어 그대로 남는다.
     *
     * <p>실패 문구가 «등록에 실패했습니다» 인 것도 레거시 그대로다.
     *
     * @param id 사번
     * @param name 이름
     * @param email 이메일
     * @param postalCode 우편번호
     * @param address1 도로명 주소
     * @param address2 상세 주소
     * @param job 직급 번호
     * @param dept 부서 번호
     * @param extensionPhone 내선 번호
     * @param mobilePhone 휴대 전화
     * @param model 뷰 모델
     * @return 결과 템플릿
     */
    @PostMapping("/update-proc")
    public String update(
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String postalCode,
            @RequestParam(required = false) String address1,
            @RequestParam(required = false) String address2,
            @RequestParam(required = false) Integer job,
            @RequestParam(required = false) Integer dept,
            @RequestParam(required = false) String extensionPhone,
            @RequestParam(required = false) String mobilePhone,
            Model model) {

        boolean given = id != null && name != null && email != null && postalCode != null
                && address1 != null && address2 != null && job != null && dept != null
                && extensionPhone != null;
        boolean updated = given && adminUserService.update(new AdminUserEdit(
                blankToNull(id), blankToNull(name), null, blankToNull(email),
                postalCode, blankToNull(address1), blankToNull(address2),
                job, dept, extensionPhone, blankToNull(mobilePhone)));

        return result(model, updated ? "수정에 성공했습니다." : "등록에 실패했습니다.",
                "/admin/user/list");
    }

    /**
     * 비밀번호 초기화 처리 (D-134).
     *
     * <p>레거시에 없던 기능이다. 임시 비밀번호를 걸고, 그 평문을 <b>이 응답 한 번만</b>
     * 보여 준다 — 새로 고치면 새 임시 비밀번호가 걸릴 뿐, 지나간 값은 어디에도 없다.
     *
     * @param id 사번
     * @param model 뷰 모델
     * @return 임시 비밀번호 안내 템플릿. 대상이 없으면 잘못된 접근 화면
     */
    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam(required = false) String id, Model model) {
        AdminUserService.TempPassword reset = id == null ? null
                : adminUserService.resetPassword(id);
        if (reset == null) {
            return "redirect:/access-error";
        }
        model.addAttribute("reset", reset);
        return "admin/user/reset-password";
    }

    /**
     * 레거시가 값을 다듬는 방식.
     *
     * <p>{@code value.trim().equals("") ? null : value} 였다 — 판정만 다듬고 <b>저장하는
     * 값은 다듬은 쪽</b>이다({@code value = value.trim()} 을 먼저 하기 때문이다).
     */
    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String result(Model model, String message, String nextPage) {
        model.addAttribute("message", message);
        model.addAttribute("nextPage", nextPage);
        return "admin/user/result";
    }
}
