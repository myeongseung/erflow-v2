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
 * admin/user/userAddress.jsp     GET  /admin/user/address?id=
 * admin/user/userRegister.jsp    GET  /admin/user/register
 * admin/user/userRegisterProc    POST /admin/user/register-proc
 * admin/user/userUpdate.jsp      GET  /admin/user/update?id=
 * admin/user/userUpdateProc      POST /admin/user/update-proc
 * </pre>
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

        AdminUserSearch search = new AdminUserSearch(
                keyfield == null ? "" : keyfield, keyword == null ? "" : keyword);
        AdminUserService.UserPage page = adminUserService.list(search, nowPage);

        model.addAttribute("users", page.rows());
        model.addAttribute("page", page.pagination());
        model.addAttribute("keyfield", search.keyfield());
        model.addAttribute("keyword", search.keyword());
        return "admin/user/list";
    }

    /**
     * 주소 팝업.
     *
     * <p>레거시는 사원을 찾지 못하면 로그인 화면으로 보내려 했다. 그런데 가리키는
     * 주소가 없는 파일이고({@code ../../login.jsp}), {@code return} 이 없어 그 전에
     * {@code NullPointerException} 으로 죽는다. 죽음은 옮기지 않고 다른 화면과 같은
     * 잘못된 접근 안내로 보낸다(D-033 과 같은 판단).
     *
     * @param id 사번
     * @param model 뷰 모델
     * @return 주소 팝업 템플릿. 대상이 없으면 잘못된 접근 화면
     */
    @GetMapping("/address")
    public String address(@RequestParam(required = false) String id, Model model) {
        AdminUserAddress address = id == null ? null : adminUserService.address(id);
        if (address == null) {
            return "redirect:/access-error";
        }
        model.addAttribute("address", address);
        return "admin/user/address";
    }

    /**
     * 사원 추가 화면.
     *
     * @param model 뷰 모델
     * @return 추가 템플릿
     */
    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("options", adminUserService.registerForm());
        return "admin/user/register";
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
     * 사원 수정 화면.
     *
     * <p>레거시는 사번이 없거나 그런 사원이 없으면 <b>빈 화면</b>을 냈다({@code return;}
     * 만 하고 아무것도 그리지 않는다). 다른 수정 화면과 같이 잘못된 접근으로 보낸다.
     *
     * @param id 사번
     * @param model 뷰 모델
     * @return 수정 템플릿. 대상이 없으면 잘못된 접근 화면
     */
    @GetMapping("/update")
    public String updateForm(@RequestParam(required = false) String id, Model model) {
        AdminUserService.UserForm form = id == null ? null : adminUserService.updateForm(id);
        if (form == null) {
            return "redirect:/access-error";
        }
        model.addAttribute("user", form.user());
        model.addAttribute("options", form.options());
        return "admin/user/update";
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
                updated ? "/admin/user/list" : "/admin/user/update");
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
